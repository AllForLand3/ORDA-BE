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

    /**
     * 후속 분석에서 재사용하는 단일 좌표 판독 진입점이다. EPSG:4326의 경도·위도 순서이며 결과 고도는 m다. 파일 경로는 DB 등록값만 사용한다. 결측 상태와
     * 파일·설정 오류 예외를 구분하며, Reader 자원은 호출 안에서 해제하고 결과에는 숫자와 상태만 반환한다.
     */
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

    /** RAW·READY 모두 허용하되 이후 경로·내부 CRS·밴드·NoData 검증을 통과해야 판독한다. */
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
