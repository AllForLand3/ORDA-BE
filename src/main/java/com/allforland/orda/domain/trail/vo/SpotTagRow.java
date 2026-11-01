package com.allforland.orda.domain.trail.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

/** 상세지점 태그 행이다. */
@Getter
@Accessors(fluent = true)
@AllArgsConstructor
public class SpotTagRow {
    private final Long spotId;
    private final String tag;
    private final String verifyStatus;
    private final String extractType;
}
