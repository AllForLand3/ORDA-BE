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

@Component
public class CourseEditTagService {
    private final ObjectMapper canonicalMapper =
            JsonMapper.builder().enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY).build();

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

    public void verify(String expected, CourseDetailResponse current) {
        if (!tag(current).equals(expected)) {
            throw new BusinessException(ErrorCode.COURSE_EDIT_CONFLICT);
        }
    }
}
