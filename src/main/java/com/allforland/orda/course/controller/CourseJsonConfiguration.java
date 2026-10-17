package com.allforland.orda.course.controller;

import com.allforland.orda.course.dto.CourseCreateRequest;
import com.allforland.orda.course.dto.CourseUpdateRequest;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** 코스 요청 JSON을 엄격하게 파싱하도록 설정한다. */
@Configuration
public class CourseJsonConfiguration implements WebMvcConfigurer {
    private final ObjectMapper objectMapper;

    public CourseJsonConfiguration(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        ObjectMapper strictMapper = objectMapper.copy();
        strictMapper.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        strictMapper.enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
        strictMapper.enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
        converters.add(0, new CourseRequestConverter(strictMapper));
    }

    private static class CourseRequestConverter extends MappingJackson2HttpMessageConverter {
        CourseRequestConverter(ObjectMapper mapper) {
            super(mapper);
        }

        @Override
        protected Object readInternal(Class<?> type, HttpInputMessage inputMessage)
                throws IOException {
            byte[] body = inputMessage.getBody().readAllBytes();
            JsonNode request = getObjectMapper().readTree(body);
            validateStringFields(request, type);
            return getObjectMapper().readValue(body, type);
        }

        @Override
        public boolean canRead(Class<?> type, MediaType mediaType) {
            return courseRequest(type) && super.canRead(type, mediaType);
        }

        @Override
        public boolean canRead(Type type, Class<?> contextClass, MediaType mediaType) {
            return courseRequest(type) && super.canRead(type, contextClass, mediaType);
        }

        @Override
        public boolean canWrite(Class<?> type, MediaType mediaType) {
            return false;
        }

        @Override
        public boolean canWrite(Type type, Class<?> valueType, MediaType mediaType) {
            return false;
        }

        private boolean courseRequest(Type type) {
            return type == CourseCreateRequest.class || type == CourseUpdateRequest.class;
        }

        private void validateStringFields(JsonNode request, Class<?> type)
                throws JsonProcessingException {
            if (request == null || !request.isObject()) {
                return;
            }

            String[] stringFields =
                    type == CourseCreateRequest.class
                            ? new String[] {"mountainId", "courseName", "memo", "createType"}
                            : new String[] {"courseName", "memo"};
            for (String field : stringFields) {
                JsonNode value = request.get(field);
                if (value != null && !value.isNull() && !value.isTextual()) {
                    throw JsonMappingException.from(
                            (JsonParser) null, "Expected a JSON string for field '" + field + "'.");
                }
            }
        }
    }
}
