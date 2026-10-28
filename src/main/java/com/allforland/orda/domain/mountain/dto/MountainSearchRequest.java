package com.allforland.orda.domain.mountain.dto;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MountainSearchRequest {

    private String keyword;

    @Min(0)
    private int page = 0;

    @Min(1)
    @Max(100)
    private int size = 20;

    // BBOX 검색 조건 (EPSG:4326)
    private Double minLng;
    private Double minLat;
    private Double maxLng;
    private Double maxLat;
}
