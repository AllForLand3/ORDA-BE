package com.allforland.orda.common.geo;

import com.allforland.orda.common.error.BusinessException;
import com.allforland.orda.common.error.ErrorCode;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LineStringGeometryDeserializer extends JsonDeserializer<LineStringGeometry> {
    @Override
    public LineStringGeometry deserialize(JsonParser parser, DeserializationContext context)
            throws IOException {
        JsonNode node = context.readTree(parser);
        if (!node.isObject()
                || node.size() != 2
                || !node.path("type").isTextual()
                || !"LineString".equals(node.path("type").textValue())
                || !node.path("coordinates").isArray()) {
            throw invalid();
        }
        List<List<Double>> coordinates = new ArrayList<>();
        for (JsonNode point : node.get("coordinates")) {
            if (!point.isArray()
                    || point.size() != 2
                    || !point.get(0).isNumber()
                    || !point.get(1).isNumber()) {
                throw invalid();
            }
            coordinates.add(List.of(point.get(0).doubleValue(), point.get(1).doubleValue()));
        }
        return new LineStringGeometry("LineString", coordinates);
    }

    private BusinessException invalid() {
        return new BusinessException(ErrorCode.INVALID_COURSE_GEOMETRY);
    }
}
