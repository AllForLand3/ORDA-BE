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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 경로 탐색 입력 검증과 후보 생성을 처리한다. */
@Service
public class RouteService {
    private static final String DIST_FIRST = "DIST_FIRST";
    private static final String LESS_STAIR = "LESS_STAIR";
    // 요청 순서와 관계없이 이 순서로 탐색하고 순위를 매긴다.
    private static final List<String> SUPPORTED_CONDITIONS = List.of(DIST_FIRST, LESS_STAIR);
    private static final String DECIMAL = "[-+]?([0-9]+(\\.[0-9]*)?|\\.[0-9]+)([eE][-+]?[0-9]+)?";
    private final RouteMapper mapper;
    private final ObjectMapper objectMapper;
    private final double snapMaxDistanceM;
    private final double stairPenaltyM;

    public RouteService(
            RouteMapper mapper,
            ObjectMapper objectMapper,
            @Value("${orda.route.snap-max-distance-m:300}") double snapMaxDistanceM,
            @Value("${orda.route.stair-penalty-m:300}") double stairPenaltyM) {
        if (!Double.isFinite(snapMaxDistanceM) || snapMaxDistanceM <= 0) {
            throw new IllegalArgumentException("orda.route.snap-max-distance-m must be positive");
        }
        if (!Double.isFinite(stairPenaltyM) || stairPenaltyM <= 0) {
            throw new IllegalArgumentException("orda.route.stair-penalty-m must be positive");
        }
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.snapMaxDistanceM = snapMaxDistanceM;
        this.stairPenaltyM = stairPenaltyM;
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

    /** 출발·도착 노드를 검증하고 요청한 탐색 기준별 경로 후보를 반환한다. */
    @Transactional(readOnly = true)
    public RouteSearchResponse search(RouteSearchRequest request) {
        if (request == null) {
            throw invalidRequest();
        }
        long mountainId = positiveId(request.mountainId());
        long startNodeId = positiveId(request.startNodeId());
        long endNodeId = positiveId(request.endNodeId());
        List<String> conditions = priorityConditions(request.priorityConditions());
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
        List<RouteCandidateResponse> candidates = new ArrayList<>();
        Set<List<String>> seenPaths = new HashSet<>();
        for (String condition : conditions) {
            double penalty = LESS_STAIR.equals(condition) ? stairPenaltyM : 0;
            RoutePathRow path = mapper.findShortestPath(startNodeId, endNodeId, penalty);
            if (path == null || path.edgeCount() == null || path.edgeCount() < 1) {
                // 비용만 다를 뿐 같은 간선 집합을 쓰므로 한 기준에서 경로가 없으면 모두 없다.
                throw new BusinessException(ErrorCode.ROUTE_NOT_FOUND);
            }
            List<String> edgeIds = edgeIds(path);
            // 다른 기준과 같은 경로면 앞 순위 후보 하나만 남긴다.
            if (seenPaths.add(edgeIds)) {
                candidates.add(candidate(candidates.size() + 1, condition, path, edgeIds));
            }
        }
        return new RouteSearchResponse(
                Long.toString(mountainId),
                Long.toString(startNodeId),
                Long.toString(endNodeId),
                List.copyOf(candidates));
    }

    /** 탐색 기준 목록을 검증하고 탐색 순서대로 정렬한다. 없거나 비어 있으면 거리 우선만 쓴다. */
    private List<String> priorityConditions(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of(DIST_FIRST);
        }
        Set<String> requested = new HashSet<>();
        for (String value : values) {
            if (value == null || !SUPPORTED_CONDITIONS.contains(value) || !requested.add(value)) {
                throw invalidRequest();
            }
        }
        return SUPPORTED_CONDITIONS.stream().filter(requested::contains).toList();
    }

    /** 탐색 결과 행의 간선 ID 목록을 검증해 반환한다. */
    private List<String> edgeIds(RoutePathRow path) {
        if (path.edgeIds() == null) {
            throw new IllegalStateException("Incomplete route path result");
        }
        List<String> edgeIds = Arrays.asList(path.edgeIds().split(","));
        if (edgeIds.size() != path.edgeCount()) {
            throw new IllegalStateException("Inconsistent route path result");
        }
        return List.copyOf(edgeIds);
    }

    /** 탐색 결과 행을 검증해 경로 후보 응답으로 변환한다. */
    private RouteCandidateResponse candidate(
            int rank, String condition, RoutePathRow path, List<String> edgeIds) {
        if (path.lengthM() == null || path.stairCount() == null || path.geometryJson() == null) {
            throw new IllegalStateException("Incomplete route path result");
        }
        LineStringGeometry geometry;
        try {
            geometry = objectMapper.readValue(path.geometryJson(), LineStringGeometry.class);
        } catch (JsonProcessingException | BusinessException exception) {
            throw new IllegalStateException("Invalid route path geometry");
        }
        return new RouteCandidateResponse(
                rank,
                condition,
                path.lengthM(),
                path.estTimeMin(),
                path.stairCount(),
                edgeIds,
                geometry);
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
