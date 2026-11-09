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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 코스 주변 상세지점·시설과 단건 상세 조회 규칙을 처리한다. */
@Service
@RequiredArgsConstructor
public class TrailQueryService {
    /** 검색 반경 기본값(m). 피그마 기준이며 팀 확정 전이다. */
    static final int DEFAULT_RADIUS_M = 200;

    /** 검색 반경 상한(m). 시설은 서비스 영역 500m 이내만 적재되어 있어 그 이상은 자료 없음과 구분할 수 없다. */
    static final int MAXIMUM_RADIUS_M = 500;

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
        Map<Long, List<SpotTagItem>> tags = tagsBySpot(rows, statusFilter);

        // 계단 등 태그 수는 태그 Point 개수가 아니라 해당 태그를 가진 대표 상세지점 수다.
        Map<String, Integer> counts = new TreeMap<>();
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

        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String facilityType : FACILITY_TYPES) {
            if (typeFilter == null || typeFilter.equals(facilityType)) {
                counts.put(facilityType, 0);
            }
        }
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
        return new NearbyFacilitiesResponse(
                Long.toString(course.courseId()),
                course.revNo(),
                radius,
                mapper.facilityDataExists(course.mountainId(), typeFilter),
                counts,
                items);
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
