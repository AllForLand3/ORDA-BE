package com.allforland.orda.common.geo;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;
import lombok.Getter;
import lombok.experimental.Accessors;

/** API에서 사용하는 GeoJSON Point다. 좌표는 EPSG:4326 경도·위도 순서다. */
@Getter
@Accessors(fluent = true)
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonPropertyOrder({"type", "coordinates"})
public class PointGeometry {
    private final String type;
    private final List<Double> coordinates;

    public PointGeometry(double longitude, double latitude) {
        this.type = "Point";
        this.coordinates = List.of(longitude, latitude);
    }
}
