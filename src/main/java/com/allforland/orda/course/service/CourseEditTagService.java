package com.allforland.orda.course.service;

import com.allforland.orda.common.error.BusinessException;
import com.allforland.orda.common.error.ErrorCode;
import com.allforland.orda.course.dto.CourseDetailResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

/** 코스 ETag 생성과 If-Match 검증을 담당한다. */
@Component
public class CourseEditTagService {
    private final ObjectMapper canonicalMapper =
            JsonMapper.builder().enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY).build();

    /** 코스 응답의 ETag를 계산한다. */
    public String tag(CourseDetailResponse course) {
        try {
            byte[] representation = canonicalMapper.writeValueAsBytes(course);
            return "\""
                    + HexFormat.of()
                            .formatHex(MessageDigest.getInstance("SHA-256").digest(representation))
                    + "\"";
        } catch (JsonProcessingException | NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Cannot calculate course ETag");
        }
    }

    /** If-Match 형식을 검증하고 태그를 반환한다. */
    public String requireIfMatch(String header) {
        if (header == null) {
            throw new BusinessException(ErrorCode.COURSE_PRECONDITION_REQUIRED);
        }
        String tag = header.strip();
        if (!tag.matches("\"[\\x21\\x23-\\x7E\\x80-\\xFF]*\"")) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        return tag;
    }

    /** 요청 태그가 최신 코스 내용과 일치하는지 확인한다. */
    public void verify(String expected, CourseDetailResponse current) {
        if (!tag(current).equals(expected)) {
            throw new BusinessException(ErrorCode.COURSE_EDIT_CONFLICT);
        }
    }
}
