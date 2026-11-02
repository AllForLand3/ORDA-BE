package com.allforland.orda.domain.course.dto;

import java.math.BigDecimal;

/** 내 코스 목록에 표시할 요약 정보다. 식별자는 문자열로 반환한다. */
public record CourseListItemResponse(
        String courseId,
        String mountainId,
        String mountainName,
        String courseName,
        String memo,
        String createType,
        BigDecimal lengthM,
        BigDecimal estTimeMin) {}
