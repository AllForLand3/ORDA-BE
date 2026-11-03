package com.allforland.orda.domain.course.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.Instant;

/** 내 코스 목록 요약이다. 식별자는 문자열이며, 저장일 미상·난이도 미산정은 null로 반환한다. */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record CourseListItemResponse(
        String courseId,
        String mountainId,
        String mountainName,
        String courseName,
        String memo,
        String createType,
        BigDecimal lengthM,
        BigDecimal estTimeMin,
        Instant savedAt,
        String difficultyCode,
        CourseNearbySummaryResponse nearby) {}
