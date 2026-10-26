package com.allforland.orda.domain.route.controller;

import com.allforland.orda.domain.route.dto.RouteSearchRequest;
import com.allforland.orda.domain.route.dto.RouteSearchResponse;
import com.allforland.orda.domain.route.dto.RouteSnapResponse;
import com.allforland.orda.domain.route.service.RouteService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 경로 탐색 HTTP 요청을 처리한다. */
@RestController
@RequestMapping("/routes")
public class RouteController {
    private final RouteService service;

    public RouteController(RouteService service) {
        this.service = service;
    }

    /** 출발·도착 노드 사이의 경로 후보를 탐색한다. */
    @PostMapping("/search")
    public ResponseEntity<RouteSearchResponse> search(@RequestBody RouteSearchRequest request) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(service.search(request));
    }

    /** 지도 좌표를 가장 가까운 통행 가능 경로 노드로 스냅한다. */
    @GetMapping("/snap")
    public ResponseEntity<RouteSnapResponse> snap(
            @RequestParam(required = false) String mountainId,
            @RequestParam(required = false) String longitude,
            @RequestParam(required = false) String latitude) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(service.snap(mountainId, longitude, latitude));
    }
}
