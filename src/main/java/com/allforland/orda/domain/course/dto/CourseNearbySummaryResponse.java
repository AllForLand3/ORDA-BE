package com.allforland.orda.domain.course.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 코스 주변에 등록된 지점과 시설의 집계 결과다. 자료 유무는 같은 산의 사용 가능한 등록 자료 기준이며 전수 조사 여부를 뜻하지 않는다. 해당 자료가 없으면 개수는 null,
 * 자료가 있지만 반경 내 일치 항목이 없으면 0이다. 조망·계단 수는 대표 지점 수이며, 시설 수는 화장실·주차장 시설 기준이다.
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record CourseNearbySummaryResponse(
        int radiusM,
        boolean spotDataAvailable,
        boolean facilityDataAvailable,
        Long viewSpotCount,
        Long stairSpotCount,
        Long toiletCount,
        Long parkingCount) {}
