package com.allforland.orda.domain.mountain.service;

import com.allforland.orda.common.dto.PageResponse;
import com.allforland.orda.domain.mountain.dto.MountainDetailResponse;
import com.allforland.orda.domain.mountain.dto.MountainListItem;
import com.allforland.orda.domain.mountain.dto.MountainSearchRequest;
import com.allforland.orda.domain.mountain.dto.ServiceAreaRow;
import com.allforland.orda.domain.mountain.mapper.MountainMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class MountainService {

    private final ObjectMapper objectMapper;
    private final MountainMapper mountainMapper;

    /*
     * 검색어 및 BBOX 조건에 따라 산 목록을 조회하고 페이지네이션 정보 반환
     *
     * @param request 산 검색어, BBOX 좌표 및 페이지네이션 조건
     * @return 산 목록과 페이지네시연 정보를 포함한 응답
     */
    public PageResponse<MountainListItem> getMountains(MountainSearchRequest request) {
        String keyword = request.getKeyword();
        if (keyword != null) {
            request.setKeyword(keyword.trim());
        }

        validateBbox(request);

        int page = request.getPage();
        int size = request.getSize();

        long offset = (long) page * size;

        List<MountainListItem> items = mountainMapper.findAll(request, size, offset);
        long totalElements = mountainMapper.count(request);

        int totalPages = (int) ((totalElements + size - 1) / size);

        return new PageResponse<>(items, page, size, totalElements, totalPages);
    }

    /*
     * 산 ID를 기준으로 산 상세 정보를 조회
     *
     * @param mountainId 조회할 산 ID
     * @return 조회된 산의 상세 정보
     * @throws ResponseStatusException 산 정보가 존재하지 않는 경우
     */
    public MountainDetailResponse getMountain(Long mountainId) {

        MountainDetailResponse mountain = mountainMapper.findById(mountainId);

        if (mountain == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "산 정보를 찾을 수 없습니다.");
        }

        return mountain;
    }

    /*
     * 산별 서비스 가능 영역을 GeoJSON FeatureCollection 형식으로 조회
     *
     * @param mountainId 조회할 산 ID
     * @return 서비스 가능 영역 GeoJSON FeatureCollection
     */
    public JsonNode getServiceAreaGeoJson(Long mountainId) {
        getMountain(mountainId);

        List<ServiceAreaRow> serviceAreas = mountainMapper.findServiceAreas(mountainId);

        ObjectNode featureCollection = objectMapper.createObjectNode();
        featureCollection.put("type", "FeatureCollection");

        ArrayNode features = featureCollection.putArray("features");

        for (ServiceAreaRow serviceArea : serviceAreas) {
            try {
                ObjectNode feature = objectMapper.createObjectNode();

                feature.put("type", "Feature");
                feature.put("id", serviceArea.getAreaId());
                feature.set("geometry", objectMapper.readTree(serviceArea.getGeometryJson()));

                ObjectNode properties = feature.putObject("properties");
                properties.put("areaId", serviceArea.getAreaId());
                properties.put("areaName", serviceArea.getAreaName());
                properties.put("mountainId", serviceArea.getMountainId());
                properties.put("datasetId", serviceArea.getDatasetId());

                features.add(feature);
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("서비스 가능 영역 GeoJSON 변환에 실패했습니다.", e);
            }
        }

        return featureCollection;
    }

    /*
     * BBOX 좌표의 입력 여부와 유효성을 검증
     *
     * BBOX는 최소 경도, 최소 위도, 최대 경도, 최대 위도 4개의 값으로 구성됨
     * 좌표를 모두 생략하면 공간 검색 조건 없이 조회하며,
     * 일부만 입력하거나 좌표 범위가 올바르지 않으면 400 오류 반환
     *
     * @param request BBOX 좌표가 포함된 산 검색 요청
     * @throws ResponseStatusException BBOX 좌표가 누락되거나 유효하지 않은 경우
     */
    private void validateBbox(MountainSearchRequest request) {
        Double minLng = request.getMinLng();
        Double minLat = request.getMinLat();
        Double maxLng = request.getMaxLng();
        Double maxLat = request.getMaxLat();

        int provided = 0;
        if (minLng != null) {
            provided++;
        }
        if (minLat != null) {
            provided++;
        }
        if (maxLng != null) {
            provided++;
        }
        if (maxLat != null) {
            provided++;
        }

        if (provided == 0) {
            return;
        }

        if (provided != 4) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "BBOX 좌표는 4개 모두 입력해야 합니다.");
        }

        if (!Double.isFinite(minLng)
                || !Double.isFinite(minLat)
                || !Double.isFinite(maxLng)
                || !Double.isFinite(maxLat)
                || minLng < -180
                || maxLng > 180
                || minLat < -90
                || maxLat > 90
                || minLng >= maxLng
                || minLat >= maxLat) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "유효하지 않은 BBOX 좌표입니다.");
        }
    }
}
