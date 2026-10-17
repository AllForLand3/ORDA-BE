package com.allforland.orda.course.controller;

import com.allforland.orda.common.error.BusinessException;
import com.allforland.orda.common.error.ErrorCode;
import com.allforland.orda.course.dto.CourseCreateRequest;
import com.allforland.orda.course.dto.CourseUpdateRequest;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

@ControllerAdvice
public class CourseRequestBodyAdvice extends RequestBodyAdviceAdapter {
    private final int maximumBytes;

    public CourseRequestBodyAdvice(
            @Value("${orda.course.max-body-bytes:2097152}") int maximumBytes) {
        if (maximumBytes < 1 || maximumBytes == Integer.MAX_VALUE) {
            throw new IllegalArgumentException("orda.course.max-body-bytes is out of range");
        }
        this.maximumBytes = maximumBytes;
    }

    @Override
    public boolean supports(
            MethodParameter parameter,
            Type targetType,
            Class<? extends HttpMessageConverter<?>> converterType) {
        return targetType == CourseCreateRequest.class || targetType == CourseUpdateRequest.class;
    }

    @Override
    public HttpInputMessage beforeBodyRead(
            HttpInputMessage input,
            MethodParameter parameter,
            Type targetType,
            Class<? extends HttpMessageConverter<?>> converterType)
            throws IOException {
        if (input.getHeaders().getContentLength() > maximumBytes) {
            throw new BusinessException(ErrorCode.PAYLOAD_TOO_LARGE);
        }
        byte[] body = input.getBody().readNBytes(maximumBytes + 1);
        if (body.length > maximumBytes) {
            throw new BusinessException(ErrorCode.PAYLOAD_TOO_LARGE);
        }
        return new HttpInputMessage() {
            @Override
            public InputStream getBody() {
                return new ByteArrayInputStream(body);
            }

            @Override
            public HttpHeaders getHeaders() {
                return input.getHeaders();
            }
        };
    }
}
