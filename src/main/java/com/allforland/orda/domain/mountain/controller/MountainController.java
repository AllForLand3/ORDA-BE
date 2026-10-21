package com.allforland.orda.domain.mountain.controller;

import com.allforland.orda.common.dto.PageResponse;
import com.allforland.orda.domain.mountain.dto.MountainDetailResponse;
import com.allforland.orda.domain.mountain.dto.MountainListItem;
import com.allforland.orda.domain.mountain.dto.MountainSearchRequest;
import com.allforland.orda.domain.mountain.service.MountainService;
import io.swagger.v3.oas.annotations.Operation;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/mountains")
@RequiredArgsConstructor
public class MountainController {

    private final MountainService mountainService;

    @Operation(summary = "산 목록 조회", description = "키워드 검색 및 페이징을 통해 서비스 대상 산 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<PageResponse<MountainListItem>> getMountains(
            @Valid @ModelAttribute MountainSearchRequest request) {
        return ResponseEntity.ok(mountainService.getMountains(request));
    }

    @Operation(summary = "산 상세 조회", description = "산 ID를 이용하여 특정 산의 상세 정보를 조회합니다.")
    @GetMapping("/{mountainId}")
    public ResponseEntity<MountainDetailResponse> getMountain(@PathVariable Long mountainId) {
        return ResponseEntity.ok(mountainService.getMountain(mountainId));
    }
}
