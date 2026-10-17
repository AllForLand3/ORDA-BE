package com.allforland.orda.common.geo;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonPropertyOrder({"type", "coordinates"})
@JsonDeserialize(using = LineStringGeometryDeserializer.class)
public class LineStringGeometry {
    private final String type;
    private final List<List<Double>> coordinates;

    public LineStringGeometry(String type, List<List<Double>> coordinates) {
        this.type = type;
        this.coordinates =
                coordinates == null ? null : coordinates.stream().map(List::copyOf).toList();
    }
}
