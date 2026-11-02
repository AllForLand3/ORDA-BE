package com.allforland.orda.domain.mountain.dto;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MountainListItem {
    private String mountainId;
    private String mountainName;
    private String addressLabel;
    private String provinceName;
    private String districtName;
    private BigDecimal elevationMeters;
    private Double longitude;
    private Double latitude;
}
