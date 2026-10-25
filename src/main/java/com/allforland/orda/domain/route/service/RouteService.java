package com.allforland.orda.domain.route.service;

import com.allforland.orda.common.error.BusinessException;
import com.allforland.orda.common.error.ErrorCode;
import com.allforland.orda.common.geo.LineStringGeometry;
import com.allforland.orda.domain.route.dto.RouteCandidateResponse;
import com.allforland.orda.domain.route.dto.RouteSearchRequest;
import com.allforland.orda.domain.route.dto.RouteSearchResponse;
import com.allforland.orda.domain.route.dto.RouteSnapResponse;
import com.allforland.orda.domain.route.mapper.RouteMapper;
import com.allforland.orda.domain.route.vo.RoutePathRow;
import com.allforland.orda.domain.route.vo.RouteSnapRow;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 경로 탐색 입력 검증과 후보 생성을 처리한다. */
@Service
public class RouteService {
    private static final String DIST_FIRST = "DIST_FIRST";
    private static final String DECIMAL = "[-+]?([0-9]+(\\.[0-9]*)?|\\.[0-9]+)([eE][-+]?[0-9]+)?";
    private final RouteMapper mapper;
    private final ObjectMapper objectMapper;
    private final double snapMaxDistanceM;

    public RouteService(
            RouteMapper mapper,
            ObjectMapper objectMapper,
            @Value("${orda.route.snap-max-distance-m:300}") double snapMaxDistanceM) {
        if (!Double.isFinite(snapMaxDistanceM) || snapMaxDistanceM <= 0) {
            throw new IllegalArgumentException("orda.route.snap-max-distance-m must be positive");
        }
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.snapMaxDistanceM = snapMaxDistanceM;
    }

    /** 좌표에서 허용 거리 안의 가장 가까운 통행 가능 노드를 찾는다. */
    @Transactional(readOnly = true)
    public RouteSnapResponse snap(String mountainId, String longitude, String latitude) {
        long id = positiveId(mountainId);
        double lon = coordinate(longitude, 180);
        double lat = coordinate(latitude, 90);
        if (!mapper.activeMountainExists(id)) {
            throw new BusinessException(ErrorCode.MOUNTAIN_NOT_FOUND);
        }
        RouteSnapRow node;
        try {
            node = mapper.findNearestNode(id, lon, lat, snapMaxDistanceM);
        } catch (DataAccessException exception) {
            if (isInputProjectionFailure(exception)) {
                throw invalidRequest();
            }
            throw exception;
        }
        if (node == null) {
            throw new BusinessException(ErrorCode.ROUTE_SNAP_FAILED);
        }
        if (node.nodeId() == null
                || node.distanceM() == null
                || node.longitude() == null
                || node.latitude() == null) {
            throw new IllegalStateException("Incomplete route snap result");
        }
        return new RouteSnapResponse(
                Long.toString(id),
                Long.toString(node.nodeId()),
                node.distanceM(),
                List.of(node.longitude(), node.latitude()));
    }

    /** 출발·도착 노드를 검증하고 거리 우선 경로 후보를 반환한다. */
    @Transactional(readOnly = true)
    public RouteSearchResponse search(RouteSearchRequest request) {
        if (request == null) {
            throw invalidRequest();
        }
        long mountainId = positiveId(request.mountainId());
        long startNodeId = positiveId(request.startNodeId());
        long endNodeId = positiveId(request.endNodeId());
        if (startNodeId == endNodeId) {
            throw new BusinessException(ErrorCode.ROUTE_SAME_POINT);
        }
        if (!mapper.activeMountainExists(mountainId)) {
            throw new BusinessException(ErrorCode.MOUNTAIN_NOT_FOUND);
        }
        // 두 노드가 요청한 산에 속하면 간선 FK(mountain_id, node_id)로 탐색도 그 산 안에 머문다.
        if (mapper.countMountainNodes(mountainId, startNodeId, endNodeId) != 2) {
            throw new BusinessException(ErrorCode.ROUTE_NODE_NOT_FOUND);
        }
        RoutePathRow path = mapper.findShortestPath(startNodeId, endNodeId);
        if (path == null || path.edgeCount() == null || path.edgeCount() < 1) {
            throw new BusinessException(ErrorCode.ROUTE_NOT_FOUND);
        }
        return new RouteSearchResponse(
                Long.toString(mountainId),
                Long.toString(startNodeId),
                Long.toString(endNodeId),
                List.of(candidate(path)));
    }

    /** 탐색 결과 행을 검증해 경로 후보 응답으로 변환한다. */
    private RouteCandidateResponse candidate(RoutePathRow path) {
        if (path.edgeIds() == null || path.lengthM() == null || path.geometryJson() == null) {
            throw new IllegalStateException("Incomplete route path result");
        }
        List<String> edgeIds = Arrays.asList(path.edgeIds().split(","));
        if (edgeIds.size() != path.edgeCount()) {
            throw new IllegalStateException("Inconsistent route path result");
        }
        LineStringGeometry geometry;
        try {
            geometry = objectMapper.readValue(path.geometryJson(), LineStringGeometry.class);
        } catch (JsonProcessingException | BusinessException exception) {
            throw new IllegalStateException("Invalid route path geometry");
        }
        return new RouteCandidateResponse(
                1, DIST_FIRST, path.lengthM(), path.estTimeMin(), List.copyOf(edgeIds), geometry);
    }

    /** 양의 long 범위의 10진 식별자를 검증한다. */
    private long positiveId(String value) {
        if (value == null || !value.matches("[0-9]+")) {
            throw invalidRequest();
        }
        try {
            long id = Long.parseLong(value);
            if (id > 0) {
                return id;
            }
        } catch (NumberFormatException ignored) {
            // Values outside the signed bigint range are invalid API identifiers.
        }
        throw invalidRequest();
    }

    /** 10진 좌표 문자열을 검증하고 절댓값 범위 안의 유한한 값으로 변환한다. */
    private double coordinate(String value, double limit) {
        if (value == null || !value.matches(DECIMAL)) {
            throw invalidRequest();
        }
        double parsed = Double.parseDouble(value);
        if (!Double.isFinite(parsed) || parsed < -limit || parsed > limit) {
            throw invalidRequest();
        }
        return parsed;
    }

    /** 사용자 좌표에 의한 투영 오류인지 구분한다. */
    private boolean isInputProjectionFailure(DataAccessException exception) {
        // Only PROJ invalid-coordinate (2049) / outside-projection-domain (2050).
        // Missing SRIDs, grid files, SQL/schema errors and connectivity remain server errors.
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sql && "XX000".equals(sql.getSQLState())) {
                String message = sql.getMessage();
                if (message != null
                        && message.contains("transform:")
                        && (message.contains("(2049)") || message.contains("(2050)"))) {
                    return true;
                }
            }
        }
        return false;
    }

    /** 일반 요청 오류를 생성한다. */
    private BusinessException invalidRequest() {
        return new BusinessException(ErrorCode.INVALID_REQUEST);
    }
}
