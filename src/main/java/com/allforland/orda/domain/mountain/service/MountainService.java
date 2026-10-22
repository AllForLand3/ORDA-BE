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

    public PageResponse<MountainListItem> getMountains(MountainSearchRequest request) {
        String keyword = request.getKeyword();
        if (keyword != null) {
            keyword = keyword.trim();
        }

        int page = request.getPage();
        int size = request.getSize();

        long offset = (long) page * size;

        List<MountainListItem> items = mountainMapper.findAll(keyword, size, offset);
        long totalElements = mountainMapper.count(keyword);

        int totalPages = (int) ((totalElements + size - 1) / size);

        return new PageResponse<>(items, page, size, totalElements, totalPages);
    }

    public MountainDetailResponse getMountain(Long mountainId) {

        MountainDetailResponse mountain = mountainMapper.findById(mountainId);

        if (mountain == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "산 정보를 찾을 수 없습니다.");
        }

        return mountain;
    }
}
