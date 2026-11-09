package com.allforland.orda.domain.trail.dto;

import com.allforland.orda.common.error.BusinessException;
import com.allforland.orda.common.error.ErrorCode;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.experimental.Accessors;

/** 경로 탐색 후보 주변 조회 요청 값을 담는다. edgeIds는 경로 탐색 응답 후보의 간선 ID 목록이다. */
@Getter
@Accessors(fluent = true)
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE)
public class RouteNearbyRequest {
    private final String mountainId;

    private final List<String> edgeIds;

    private final Integer radiusM;

    private RouteNearbyRequest(String mountainId, List<String> edgeIds, Integer radiusM) {
        this.mountainId = mountainId;
        this.edgeIds = edgeIds;
        this.radiusM = radiusM;
    }

    /** JSON 타입을 엄격하게 확인한다. ID는 JSON 문자열, radiusM은 JSON 정수만 허용한다. 값 형식·범위·필수 여부는 서비스에서 검증한다. */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static RouteNearbyRequest fromJson(JsonNode body) {
        if (body == null || !body.isObject()) {
            throw invalidRequest();
        }
        return new RouteNearbyRequest(
                text(body.get("mountainId")),
                edgeIds(body.get("edgeIds")),
                radius(body.get("radiusM")));
    }

    /** 값이 없으면 null, JSON 문자열이면 그 값을 반환하고 그 밖의 타입은 거부한다. */
    private static String text(JsonNode value) {
        if (value == null || value.isNull()) {
            return null;
        }
        if (!value.isTextual()) {
            throw invalidRequest();
        }
        return value.textValue();
    }

    /** 값이 없으면 null, 모든 원소가 JSON 문자열인 배열이면 목록을 반환하고 그 밖에는 거부한다. */
    private static List<String> edgeIds(JsonNode value) {
        if (value == null || value.isNull()) {
            return null;
        }
        if (!value.isArray()) {
            throw invalidRequest();
        }
        List<String> ids = new ArrayList<>(value.size());
        for (JsonNode item : value) {
            if (!item.isTextual()) {
                throw invalidRequest();
            }
            ids.add(item.textValue());
        }
        return List.copyOf(ids);
    }

    /** 값이 없으면 null, int 범위의 JSON 정수이면 그 값을 반환하고 소수·문자열 등은 거부한다. */
    private static Integer radius(JsonNode value) {
        if (value == null || value.isNull()) {
            return null;
        }
        if (!value.isIntegralNumber() || !value.canConvertToInt()) {
            throw invalidRequest();
        }
        return value.intValue();
    }

    private static BusinessException invalidRequest() {
        return new BusinessException(ErrorCode.INVALID_REQUEST);
    }
}
