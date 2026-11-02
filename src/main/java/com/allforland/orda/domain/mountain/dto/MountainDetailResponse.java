package com.allforland.orda.domain.mountain.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class MountainDetailResponse {

    private String mountainId;
    private String mountainName;
    private String sourceMountainCode;
    private String sourceOldMountainCode;
    private boolean serviceAvailable;
    private String addressLabel;
    private String provinceName;
    private String districtName;
    private BigDecimal elevationMeters;
    private Double longitude;
    private Double latitude;
}
