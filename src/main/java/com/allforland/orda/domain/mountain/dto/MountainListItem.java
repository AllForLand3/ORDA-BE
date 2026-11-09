package com.allforland.orda.domain.mountain.dto;

import com.allforland.orda.common.geo.PointGeometry;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
    private PointGeometry geometry;

    @JsonIgnore private Double longitude;

    @JsonIgnore private Double latitude;
}
