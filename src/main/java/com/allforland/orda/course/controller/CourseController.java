package com.allforland.orda.course.controller;

import com.allforland.orda.common.error.BusinessException;
import com.allforland.orda.common.error.ErrorCode;
import com.allforland.orda.course.dto.CourseCreateRequest;
import com.allforland.orda.course.dto.CourseDetailResponse;
import com.allforland.orda.course.dto.CourseUpdateRequest;
import com.allforland.orda.course.service.CourseEditTagService;
import com.allforland.orda.course.service.CourseService;
import java.net.URI;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/courses")
public class CourseController {
    private final CourseService service;
    private final CourseEditTagService editTags;

    public CourseController(CourseService service, CourseEditTagService editTags) {
        this.service = service;
        this.editTags = editTags;
    }

    @PostMapping
    public ResponseEntity<CourseDetailResponse> create(@RequestBody CourseCreateRequest request) {
        CourseDetailResponse course = service.create(request);
        URI location =
                ServletUriComponentsBuilder.fromCurrentRequest()
                        .path("/{courseId}")
                        .buildAndExpand(course.courseId())
                        .toUri();
        return ResponseEntity.created(location)
                .cacheControl(CacheControl.noStore())
                .eTag(editTags.tag(course))
                .body(course);
    }

    @GetMapping("/{courseId}")
    public ResponseEntity<CourseDetailResponse> get(@PathVariable String courseId) {
        CourseDetailResponse course = service.get(courseId);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .eTag(editTags.tag(course))
                .body(course);
    }

    @PatchMapping("/{courseId}")
    public ResponseEntity<CourseDetailResponse> update(
            @PathVariable String courseId,
            @RequestBody CourseUpdateRequest request,
            @RequestHeader HttpHeaders headers) {
        CourseDetailResponse course = service.update(courseId, request, ifMatch(headers));
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .eTag(editTags.tag(course))
                .body(course);
    }

    @DeleteMapping("/{courseId}")
    public ResponseEntity<Void> delete(
            @PathVariable String courseId, @RequestHeader HttpHeaders headers) {
        service.delete(courseId, ifMatch(headers));
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }

    private String ifMatch(HttpHeaders headers) {
        List<String> values = headers.get(HttpHeaders.IF_MATCH);
        if (values == null || values.isEmpty()) {
            return null;
        }
        if (values.size() != 1) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        return values.get(0);
    }
}
