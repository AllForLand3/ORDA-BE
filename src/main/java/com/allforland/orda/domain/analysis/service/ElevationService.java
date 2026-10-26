package com.allforland.orda.domain.analysis.service;

import com.allforland.orda.common.error.BusinessException;
import com.allforland.orda.common.error.ErrorCode;
import com.allforland.orda.domain.analysis.mapper.DemAssetMapper;
import com.allforland.orda.domain.analysis.vo.DemAssetRow;
import com.allforland.orda.domain.analysis.vo.ElevationResult;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ElevationService {

    private final DemAssetMapper demAssetMapper;
    private final DemFileResolver demFileResolver;
    private final DemRasterReader demRasterReader;

    /** EPSG:4326의 경도·위도에서 등록된 DEM의 고도를 판독한다. 파일 경로는 호출자가 지정하지 않고 DB에 등록된 값만 사용한다. */
    public ElevationResult extract(long demId, double longitude, double latitude) {
        validateCoordinates(longitude, latitude);

        if (demId <= 0) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        DemAssetRow dem = demAssetMapper.findById(demId);

        if (dem == null) {
            throw new BusinessException(ErrorCode.DEM_NOT_FOUND);
        }

        validateAvailability(dem);

        Path file = demFileResolver.resolve(dem.filePath());

        return demRasterReader.read(file, dem, longitude, latitude);
    }

    private void validateCoordinates(double longitude, double latitude) {
        if (!Double.isFinite(longitude)
                || !Double.isFinite(latitude)
                || longitude < -180.0
                || longitude > 180.0
                || latitude < -90.0
                || latitude > 90.0) {
            throw new BusinessException(ErrorCode.DEM_INVALID_COORDINATE);
        }
    }

    private void validateAvailability(DemAssetRow dem) {
        if (!"Y".equals(dem.useYn())) {
            throw new BusinessException(ErrorCode.DEM_UNAVAILABLE);
        }

        String status = dem.prepStatusCode();

        if (!"RAW".equals(status) && !"READY".equals(status)) {
            throw new BusinessException(ErrorCode.DEM_UNAVAILABLE);
        }
    }
}
