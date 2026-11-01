package com.allforland.orda.domain.trail.dto;

import com.allforland.orda.common.geo.PointGeometry;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

/** 코스 주변 시설 응답 항목이다. */
@Getter
@Accessors(fluent = true)
@AllArgsConstructor
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonPropertyOrder({
    "facilityId",
    "type",
    "name",
    "address",
    "distanceM",
    "locationBasis",
    "verifyStatus",
    "geometry"
})
public class NearbyFacilityItem {
    private final String facilityId;
    private final String type;
    private final String name;
    private final String address;
    private final BigDecimal distanceM;
    private final String locationBasis;
    private final String verifyStatus;
    private final PointGeometry geometry;
}
