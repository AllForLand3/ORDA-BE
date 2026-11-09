package com.allforland.orda.domain.trail.service;

import com.allforland.orda.common.error.BusinessException;
import com.allforland.orda.common.error.ErrorCode;
import com.allforland.orda.common.geo.PointGeometry;
import com.allforland.orda.domain.auth.service.AuthService;
import com.allforland.orda.domain.trail.dto.FacilityDetailResponse;
import com.allforland.orda.domain.trail.dto.FacilitySourceItem;
import com.allforland.orda.domain.trail.dto.NearbyFacilitiesResponse;
import com.allforland.orda.domain.trail.dto.NearbyFacilityItem;
import com.allforland.orda.domain.trail.dto.NearbySpotItem;
import com.allforland.orda.domain.trail.dto.NearbySpotsResponse;
import com.allforland.orda.domain.trail.dto.RouteNearbyGroup;
import com.allforland.orda.domain.trail.dto.RouteNearbyRequest;
import com.allforland.orda.domain.trail.dto.RouteNearbyResponse;
import com.allforland.orda.domain.trail.dto.SpotMatchItem;
import com.allforland.orda.domain.trail.dto.SpotTagItem;
import com.allforland.orda.domain.trail.dto.TrailSpotDetailResponse;
import com.allforland.orda.domain.trail.mapper.TrailMapper;
import com.allforland.orda.domain.trail.vo.FacilityDetailRow;
import com.allforland.orda.domain.trail.vo.NearbyFacilityRow;
import com.allforland.orda.domain.trail.vo.NearbySpotRow;
import com.allforland.orda.domain.trail.vo.SpotDetailRow;
import com.allforland.orda.domain.trail.vo.SpotTagRow;
import com.allforland.orda.domain.trail.vo.TrailCourseRow;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 코스·경로 탐색 후보 주변 상세지점·시설과 단건 상세 조회 규칙을 처리한다. */
@Service
@RequiredArgsConstructor
public class TrailQueryService {
    /** 검색 반경 기본값(m). 피그마 기준이며 팀 확정 전이다. */
    static final int DEFAULT_RADIUS_M = 200;

    /** 검색 반경 상한(m). 시설은 서비스 영역 500m 이내만 적재되어 있어 그 이상은 자료 없음과 구분할 수 없다. */
    static final int MAXIMUM_RADIUS_M = 500;

    /** 후보 간선 수 상한. 후보는 같은 간선을 두 번 지나지 않아 산 전체 간선 수(관악산 1,377개)를 넘지 않으므로 정상 후보는 막지 않는다. */
    static final int MAXIMUM_EDGE_COUNT = 2000;

    private static final Set<String> SPOT_TAGS =
            Set.of(
                    "STAIR", "STEEP", "ROPE", "DANGER", "WATER", "REST", "PEAK", "VIEW", "TOILET",
                    "PARKING", "HELIPAD");
    private static final Set<String> FILTER_VERIFY_STATUSES = Set.of("UNVERIFIED", "VERIFIED");
    private static final List<String> FACILITY_TYPES = List.of("TOILET", "PARKING");

    private final TrailMapper mapper;
    private final AuthService authService;

    /** 현재 코스 형상 반경 안의 태그 상세지점을 거리순으로 조회한다(I-01). */
    @Transactional(readOnly = true)
    public NearbySpotsResponse findNearbySpots(
            long courseId, Integer revNo, Integer radiusM, String tag, String verifyStatus) {
        int radius = radius(radiusM);
        String tagFilter = allowed(tag, SPOT_TAGS);
        String statusFilter = allowed(verifyStatus, FILTER_VERIFY_STATUSES);
        TrailCourseRow course = requireCourse(courseId, revNo);

        List<NearbySpotRow> rows =
                mapper.findNearbySpots(courseId, radius, tagFilter, statusFilter);
        Map<String, Integer> counts = new TreeMap<>();
        List<NearbySpotItem> items = spotItems(rows, statusFilter, counts);
        return new NearbySpotsResponse(
                Long.toString(course.courseId()),
                course.revNo(),
                radius,
                mapper.spotDataExists(course.mountainId()),
                counts,
                items);
    }

    /** 현재 코스 형상 반경 안의 시설을 거리순으로 조회한다(I-02). */
    @Transactional(readOnly = true)
    public NearbyFacilitiesResponse findNearbyFacilities(
            long courseId, Integer revNo, Integer radiusM, String type) {
        int radius = radius(radiusM);
        String typeFilter = allowed(type, Set.copyOf(FACILITY_TYPES));
        TrailCourseRow course = requireCourse(courseId, revNo);

        List<NearbyFacilityRow> rows = mapper.findNearbyFacilities(courseId, radius, typeFilter);

        Map<String, Integer> counts = facilityCounts(typeFilter);
        List<NearbyFacilityItem> items = facilityItems(rows, counts);
        return new NearbyFacilitiesResponse(
                Long.toString(course.courseId()),
                course.revNo(),
                radius,
                mapper.facilityDataExists(course.mountainId(), typeFilter),
                counts,
                items);
    }

    /** 저장 전 경로 탐색 후보의 간선 반경 안 상세지점·시설을 거리순으로 조회한다. 기준은 I-01·I-02와 같다. */
    @Transactional(readOnly = true)
    public RouteNearbyResponse findRouteNearby(RouteNearbyRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        long mountainId = positiveId(request.mountainId());
        List<Long> edgeIds = edgeIds(request.edgeIds());
        int radius = radius(request.radiusM());
        authService.requireCurrentUser();
        if (!mapper.activeMountainExists(mountainId)) {
            throw new BusinessException(ErrorCode.MOUNTAIN_NOT_FOUND);
        }
        // 없는 간선, 다른 산 간선, 통행 불가 간선이 하나라도 있으면 경로 탐색 결과가 아니다.
        if (mapper.countRouteEdges(mountainId, edgeIds) != edgeIds.size()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        Map<String, Integer> spotCounts = new TreeMap<>();
        List<NearbySpotItem> spots =
                spotItems(
                        mapper.findRouteNearbySpots(mountainId, edgeIds, radius), null, spotCounts);
        Map<String, Integer> facilityCounts = facilityCounts(null);
        List<NearbyFacilityItem> facilities =
                facilityItems(
                        mapper.findRouteNearbyFacilities(mountainId, edgeIds, radius),
                        facilityCounts);
        return new RouteNearbyResponse(
                Long.toString(mountainId),
                radius,
                new RouteNearbyGroup<>(mapper.spotDataExists(mountainId), spotCounts, spots),
                new RouteNearbyGroup<>(
                        mapper.facilityDataExists(mountainId, null), facilityCounts, facilities));
    }

    /** 상세지점의 위치·태그·검수·매칭 정보를 조회한다(I-03). 검수 정보이므로 REJECTED 태그도 포함한다. */
    @Transactional(readOnly = true)
    public TrailSpotDetailResponse findSpot(long spotId) {
        authService.requireCurrentUser();
        SpotDetailRow spot = mapper.findSpot(spotId);
        if (spot == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        List<SpotTagItem> tags =
                mapper.findSpotTags(List.of(spotId), true, null).stream()
                        .map(this::tagItem)
                        .toList();
        List<SpotMatchItem> matches =
                mapper.findSpotMatches(spotId).stream()
                        .map(
                                match ->
                                        new SpotMatchItem(
                                                Long.toString(match.edgeId()),
                                                match.distanceM(),
                                                match.positionRatio(),
                                                match.matchType(),
                                                match.matchStatus()))
                        .toList();
        return new TrailSpotDetailResponse(
                Long.toString(spot.spotId()),
                Long.toString(spot.mountainId()),
                Long.toString(spot.datasetId()),
                spot.sourceSpotId(),
                spot.sourceClass(),
                spot.detail(),
                spot.etc(),
                spot.verifyStatus(),
                idText(spot.duplicateOfSpotId()),
                tags,
                matches,
                point(spot.longitude(), spot.latitude()));
    }

    /** 시설의 위치·유형·출처·검수 정보를 조회한다(I-04). */
    @Transactional(readOnly = true)
    public FacilityDetailResponse findFacility(long facilityId) {
        authService.requireCurrentUser();
        FacilityDetailRow facility = mapper.findFacility(facilityId);
        if (facility == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        return new FacilityDetailResponse(
                Long.toString(facility.facilityId()),
                Long.toString(facility.mountainId()),
                facility.type(),
                facility.name(),
                facility.address(),
                facility.locationBasis(),
                facility.verifyStatus(),
                idText(facility.duplicateOfFacilityId()),
                new FacilitySourceItem(
                        Long.toString(facility.datasetId()),
                        facility.datasetName(),
                        facility.provider(),
                        facility.baseDate(),
                        facility.collectDate()),
                point(facility.longitude(), facility.latitude()));
    }

    /** 로그인 사용자의 코스인지, 요청 버전이 현재 버전과 같은지 확인한다. */
    private TrailCourseRow requireCourse(long courseId, Integer revNo) {
        if (revNo != null && revNo < 1) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        long owner = authService.requireCurrentUser().userId();
        TrailCourseRow course = mapper.findOwnedCourse(courseId, owner);
        if (course == null) {
            throw new BusinessException(ErrorCode.COURSE_NOT_FOUND);
        }
        if (!Boolean.TRUE.equals(course.geometryValid())
                || course.revNo() == null
                || course.mountainId() == null) {
            throw new BusinessException(ErrorCode.COURSE_STATE_UNAVAILABLE);
        }
        if (revNo != null && !revNo.equals(course.revNo())) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }
        return course;
    }

    /** 검색 반경을 기본값으로 채우고 허용 범위를 검증한다. */
    private int radius(Integer radiusM) {
        if (radiusM == null) {
            return DEFAULT_RADIUS_M;
        }
        if (radiusM < 1 || radiusM > MAXIMUM_RADIUS_M) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        return radiusM;
    }

    /** 후보 간선 ID 목록을 검증한다. 경로 탐색 후보는 같은 간선을 두 번 지나지 않으므로 중복은 잘못된 요청이다. */
    private List<Long> edgeIds(List<String> values) {
        if (values == null || values.isEmpty() || values.size() > MAXIMUM_EDGE_COUNT) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        Set<Long> unique = new LinkedHashSet<>();
        for (String value : values) {
            if (!unique.add(positiveId(value))) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST);
            }
        }
        return List.copyOf(unique);
    }

    /** 양의 long 범위의 10진 식별자를 검증한다. */
    private long positiveId(String value) {
        if (value == null || !value.matches("[1-9][0-9]{0,18}")) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }

    /** 상세지점 행을 응답 항목으로 바꾸고 태그별 대표 상세지점 수를 counts에 더한다. */
    private List<NearbySpotItem> spotItems(
            List<NearbySpotRow> rows, String verifyStatus, Map<String, Integer> counts) {
        Map<Long, List<SpotTagItem>> tags = tagsBySpot(rows, verifyStatus);

        // 계단 등 태그 수는 태그 Point 개수가 아니라 해당 태그를 가진 대표 상세지점 수다.
        List<NearbySpotItem> items = new ArrayList<>();
        for (NearbySpotRow row : rows) {
            List<SpotTagItem> spotTags = tags.getOrDefault(row.spotId(), List.of());
            spotTags.forEach(item -> counts.merge(item.tag(), 1, Integer::sum));
            items.add(
                    new NearbySpotItem(
                            Long.toString(row.spotId()),
                            row.sourceClass(),
                            row.detail(),
                            row.distanceM(),
                            row.matchDistanceM(),
                            row.verifyStatus(),
                            spotTags,
                            point(row.longitude(), row.latitude())));
        }
        return items;
    }

    /** 조회 대상 시설 유형별 개수를 0으로 채운다. 결과가 없는 유형도 0으로 응답한다. */
    private Map<String, Integer> facilityCounts(String typeFilter) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String facilityType : FACILITY_TYPES) {
            if (typeFilter == null || typeFilter.equals(facilityType)) {
                counts.put(facilityType, 0);
            }
        }
        return counts;
    }

    /** 시설 행을 응답 항목으로 바꾸고 유형별 개수를 counts에 더한다. */
    private List<NearbyFacilityItem> facilityItems(
            List<NearbyFacilityRow> rows, Map<String, Integer> counts) {
        List<NearbyFacilityItem> items = new ArrayList<>();
        for (NearbyFacilityRow row : rows) {
            counts.merge(row.type(), 1, Integer::sum);
            items.add(
                    new NearbyFacilityItem(
                            Long.toString(row.facilityId()),
                            row.type(),
                            row.name(),
                            row.address(),
                            row.distanceM(),
                            row.locationBasis(),
                            row.verifyStatus(),
                            point(row.longitude(), row.latitude())));
        }
        return items;
    }

    /** 선택 필터 값이 서버 허용 목록에 있는지 확인한다. */
    private String allowed(String value, Set<String> allowedValues) {
        if (value == null) {
            return null;
        }
        if (!allowedValues.contains(value)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        return value;
    }

    /** 조회한 상세지점들의 REJECTED가 아닌 태그를 지점별로 묶는다. */
    private Map<Long, List<SpotTagItem>> tagsBySpot(List<NearbySpotRow> rows, String verifyStatus) {
        if (rows.isEmpty()) {
            return Map.of();
        }
        List<Long> spotIds = rows.stream().map(NearbySpotRow::spotId).toList();
        return mapper.findSpotTags(spotIds, false, verifyStatus).stream()
                .collect(
                        Collectors.groupingBy(
                                SpotTagRow::spotId,
                                Collectors.mapping(this::tagItem, Collectors.toList())));
    }

    private SpotTagItem tagItem(SpotTagRow row) {
        return new SpotTagItem(row.tag(), row.verifyStatus(), row.extractType());
    }

    private PointGeometry point(Double longitude, Double latitude) {
        if (longitude == null || latitude == null) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
        return new PointGeometry(longitude, latitude);
    }

    private String idText(Long id) {
        return id == null ? null : Long.toString(id);
    }
}
