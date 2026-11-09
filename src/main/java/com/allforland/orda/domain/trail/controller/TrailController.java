package com.allforland.orda.domain.trail.controller;

import com.allforland.orda.common.error.BusinessException;
import com.allforland.orda.common.error.ErrorCode;
import com.allforland.orda.domain.trail.dto.FacilityDetailResponse;
import com.allforland.orda.domain.trail.dto.NearbyFacilitiesResponse;
import com.allforland.orda.domain.trail.dto.NearbySpotsResponse;
import com.allforland.orda.domain.trail.dto.RouteNearbyRequest;
import com.allforland.orda.domain.trail.dto.RouteNearbyResponse;
import com.allforland.orda.domain.trail.dto.TrailSpotDetailResponse;
import com.allforland.orda.domain.trail.service.TrailQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 코스·경로 탐색 후보 주변 상세지점·시설과 단건 상세 조회 HTTP 요청을 처리한다. */
@RestController
@RequiredArgsConstructor
public class TrailController {
    private final TrailQueryService service;

    /** 현재 코스 형상 반경 안의 태그 상세지점을 거리순으로 조회한다(I-01). */
    @Operation(
            summary = "코스 주변 상세지점 조회",
            description =
                    "현재 코스 형상 반경 안의 계단·쉼터·조망·급경사·로프·위험 등 태그 상세지점을 거리순으로 조회합니다."
                            + " counts는 태그별 상세지점 수이며, 검수에서 제외(REJECTED)된 지점·태그는 포함하지 않습니다.")
    @GetMapping("/courses/{courseId}/spots")
    public ResponseEntity<NearbySpotsResponse> getCourseSpots(
            @PathVariable("courseId") String courseId,
            @Parameter(description = "코스 형상 버전. 현재 버전과 다르면 409")
                    @RequestParam(value = "revNo", required = false)
                    Integer revNo,
            @Parameter(description = "검색 반경(m). 기본 200, 허용 1~500")
                    @RequestParam(value = "radiusM", required = false)
                    Integer radiusM,
            @Parameter(description = "이 태그를 가진 지점만 조회. 예: STAIR, REST, VIEW, STEEP, ROPE, DANGER")
                    @RequestParam(value = "tag", required = false)
                    String tag,
            @Parameter(description = "태그 검수 상태 필터. UNVERIFIED 또는 VERIFIED")
                    @RequestParam(value = "verifyStatus", required = false)
                    String verifyStatus) {
        NearbySpotsResponse response =
                service.findNearbySpots(positiveId(courseId), revNo, radiusM, tag, verifyStatus);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(response);
    }

    /** 현재 코스 형상 반경 안의 시설을 거리순으로 조회한다(I-02). */
    @Operation(
            summary = "코스 주변 시설 조회",
            description = "현재 코스 형상 반경 안의 화장실·주차장을 거리순으로 조회하고 유형별 개수를 함께 반환합니다.")
    @GetMapping("/courses/{courseId}/facilities")
    public ResponseEntity<NearbyFacilitiesResponse> getCourseFacilities(
            @PathVariable("courseId") String courseId,
            @Parameter(description = "코스 형상 버전. 현재 버전과 다르면 409")
                    @RequestParam(value = "revNo", required = false)
                    Integer revNo,
            @Parameter(description = "검색 반경(m). 기본 200, 허용 1~500")
                    @RequestParam(value = "radiusM", required = false)
                    Integer radiusM,
            @Parameter(description = "시설 유형 필터. TOILET 또는 PARKING")
                    @RequestParam(value = "type", required = false)
                    String type) {
        NearbyFacilitiesResponse response =
                service.findNearbyFacilities(positiveId(courseId), revNo, radiusM, type);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(response);
    }

    /** 저장 전 경로 탐색 후보 반경 안의 상세지점·시설을 거리순으로 조회한다. */
    @Operation(
            summary = "경로 탐색 후보 주변 상세지점·시설 조회",
            description =
                    "경로 탐색 응답 후보의 edgeIds로 후보 경로선을 만들고, 반경 안의 태그 상세지점(spots)과"
                            + " 화장실·주차장(facilities)을 거리순으로 조회합니다. radiusM은 기본 200, 허용 1~500입니다."
                            + " 간선이 없거나 다른 산 간선·통행 불가 간선이 있으면 400입니다."
                            + " spots·facilities의 counts·dataAvailable·items 의미는 코스 주변 조회와 같습니다.")
    @PostMapping("/routes/nearby")
    public ResponseEntity<RouteNearbyResponse> getRouteNearby(
            @RequestBody RouteNearbyRequest request) {
        RouteNearbyResponse response = service.findRouteNearby(request);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(response);
    }

    /** 상세지점의 위치·태그·검수·매칭 정보를 조회한다(I-03). */
    @Operation(summary = "상세지점 상세 조회", description = "상세지점의 위치·원본 메모·태그·검수 상태·등산로 구간 매칭 정보를 조회합니다.")
    @GetMapping("/trail-spots/{spotId}")
    public ResponseEntity<TrailSpotDetailResponse> getSpot(@PathVariable("spotId") String spotId) {
        TrailSpotDetailResponse response = service.findSpot(positiveId(spotId));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(response);
    }

    /** 시설의 위치·유형·출처·검수 정보를 조회한다(I-04). */
    @Operation(summary = "시설 상세 조회", description = "시설의 위치·유형·주소·출처 데이터셋·검수 상태를 조회합니다.")
    @GetMapping("/facilities/{facilityId}")
    public ResponseEntity<FacilityDetailResponse> getFacility(
            @PathVariable("facilityId") String facilityId) {
        FacilityDetailResponse response = service.findFacility(positiveId(facilityId));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(response);
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
}
