package com.allforland.orda.domain.mountain.dto;

import lombok.Data;

@Data
public class MountainDetailResponse {

    private String mountainId;
    private String mountainName;
    private String sourceMountainCode;
    private String sourceOldMountainCode;
    private boolean serviceAvailable;
}
