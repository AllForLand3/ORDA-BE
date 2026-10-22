package com.allforland.orda.domain.mountain.service;

import com.allforland.orda.common.dto.PageResponse;
import com.allforland.orda.domain.mountain.dto.MountainDetailResponse;
import com.allforland.orda.domain.mountain.dto.MountainListItem;
import com.allforland.orda.domain.mountain.dto.MountainSearchRequest;
import com.allforland.orda.domain.mountain.mapper.MountainMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class MountainService {

    private final MountainMapper mountainMapper;

    /*
     * 검색 조건에 따라 산 목록을 조회하고 페이지네이션 정보 반환
     *
     * @param request 산 검색어 및 페이지네이션 조건
     * @return 산 목록과 페이지네시연 정보를 포함한 응답
     */
    public PageResponse<MountainListItem> getMountains(MountainSearchRequest request) {
        String keyword = request.getKeyword();
        if (keyword != null) {
            keyword = keyword.trim();
        }

        validateBbox(request);

        String keyword = request.getKeyword();
        if (keyword != null) {
            request.setKeyword(keyword.trim());
        }

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

    private void validateBbox(MountainSearchRequest request) {
        Double minLng = request.getMinLng();
        Double minLat = request.getMinLat();
        Double maxLng = request.getMaxLng();
        Double maxLat = request.getMaxLat();

        int provided = 0;
        if (minLng != null) provided++;
        if (minLat != null) provided++;
        if (maxLng != null) provided++;
        if (maxLat != null) provided++;

        if (provided == 0) return;

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
