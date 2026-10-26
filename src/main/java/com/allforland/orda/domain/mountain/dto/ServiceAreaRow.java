package com.allforland.orda.domain.mountain.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ServiceAreaRow {
    private Long areaId;
    private Long mountainId;
    private Long datasetId;
    private String areaName;
    private String geometryJson;
}
