package com.allforland.orda.course.service;

import com.allforland.orda.common.error.BusinessException;
import com.allforland.orda.common.error.ErrorCode;
import com.allforland.orda.common.geo.LineStringGeometry;
import com.allforland.orda.course.dto.CourseCreateRequest;
import com.allforland.orda.course.dto.CourseDetailResponse;
import com.allforland.orda.course.dto.CourseUpdateRequest;
import com.allforland.orda.course.mapper.CourseMapper;
import com.allforland.orda.course.vo.CourseGeometryRow;
import com.allforland.orda.course.vo.CourseRow;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseService {
    private static final BigDecimal MAXIMUM_LENGTH = new BigDecimal("99999999.9");
    private final CourseMapper mapper;
    private final String configuredOwner;
    private final int maximumCoordinates;
    private final ObjectMapper objectMapper;
    private final CourseEditTagService editTags;

    public CourseService(
            CourseMapper mapper,
            @Value("${orda.course.workspace-owner-user-id:}") String configuredOwner,
            @Value("${orda.course.max-coordinates:10000}") int maximumCoordinates,
            ObjectMapper objectMapper,
            CourseEditTagService editTags) {
        if (maximumCoordinates < 2) {
            throw new IllegalArgumentException("orda.course.max-coordinates must be at least 2");
        }
        this.mapper = mapper;
        this.configuredOwner = configuredOwner;
        this.maximumCoordinates = maximumCoordinates;
        this.objectMapper = objectMapper;
        this.editTags = editTags;
    }

    @Transactional
    public CourseDetailResponse create(CourseCreateRequest request) {
        if (request == null) {
            throw invalidRequest();
        }
        long mountainId = positiveId(request.mountainId());
        String name = courseName(request.courseName());
        String memo = courseMemo(request.memo());
        if (!"IMPORT".equals(request.createType()) && !"DRAW".equals(request.createType())) {
            throw invalidRequest();
        }
        String geometryJson = validateAndSerializeGeometry(request.geometry());
        long owner = requireOwner();
        requireMountain(mountainId);
        CourseGeometryRow geometry = prepareGeometry(geometryJson, mountainId, null);
        Long courseId =
                mapper.insertCourse(
                        mountainId,
                        owner,
                        name,
                        memo,
                        request.createType(),
                        geometry.geometryHex(),
                        geometry.lengthM(),
                        Long.toString(owner));
        if (courseId == null || courseId <= 0) {
            throw stateUnavailable();
        }
        return detail(requireCourse(courseId, owner, false));
    }

    @Transactional(readOnly = true)
    public CourseDetailResponse get(String courseId) {
        long id = positiveId(courseId);
        long owner = requireOwner();
        return detail(requireCourse(id, owner, false));
    }

    @Transactional
    public CourseDetailResponse update(
            String courseId, CourseUpdateRequest request, String ifMatch) {
        long id = positiveId(courseId);
        String expected = editTags.requireIfMatch(ifMatch);
        if (request == null
                || (!request.namePresent()
                        && !request.memoPresent()
                        && !request.geometryPresent())) {
            throw invalidRequest();
        }
        String requestedName = request.namePresent() ? courseName(request.courseName()) : null;
        String requestedMemo = request.memoPresent() ? courseMemo(request.memo()) : null;
        String geometryJson =
                request.geometryPresent() ? validateAndSerializeGeometry(request.geometry()) : null;

        long owner = requireOwner();
        CourseRow row = requireCourse(id, owner, true);
        CourseDetailResponse current = detail(row);
        editTags.verify(expected, current);

        String name = request.namePresent() ? requestedName : row.courseName();
        String memo = request.memoPresent() ? requestedMemo : row.memo();
        CourseGeometryRow geometry = null;
        boolean geometryChanged = false;
        if (request.geometryPresent()) {
            requireMountain(row.mountainId());
            geometry = prepareGeometry(geometryJson, row.mountainId(), id);
            geometryChanged = !Boolean.TRUE.equals(geometry.sameGeometry());
        }
        if (!geometryChanged
                && Objects.equals(name, row.courseName())
                && Objects.equals(memo, row.memo())) {
            return current;
        }
        if (geometryChanged && row.revNo() == Integer.MAX_VALUE) {
            throw stateUnavailable();
        }
        int affected =
                mapper.updateCourse(
                        id,
                        owner,
                        name,
                        memo,
                        geometryChanged,
                        geometryChanged ? geometry.geometryHex() : null,
                        geometryChanged ? geometry.lengthM() : null,
                        Long.toString(owner));
        requireSingleChange(affected);
        return detail(requireCourse(id, owner, false));
    }

    @Transactional
    public void delete(String courseId, String ifMatch) {
        long id = positiveId(courseId);
        String expected = editTags.requireIfMatch(ifMatch);
        long owner = requireOwner();
        CourseDetailResponse current = detail(requireCourse(id, owner, true));
        editTags.verify(expected, current);
        requireSingleChange(mapper.deleteCourse(id, owner, Long.toString(owner)));
    }

    private void requireSingleChange(int affected) {
        if (affected != 1) {
            throw stateUnavailable();
        }
    }

    private long requireOwner() {
        long owner;
        try {
            owner = positiveId(configuredOwner);
        } catch (BusinessException exception) {
            throw new BusinessException(ErrorCode.COURSE_WORKSPACE_UNAVAILABLE);
        }
        if (!mapper.activeUserExists(owner)) {
            throw new BusinessException(ErrorCode.COURSE_WORKSPACE_UNAVAILABLE);
        }
        return owner;
    }

    private long positiveId(String value) {
        if (value == null || !value.matches("[0-9]+")) {
            throw invalidRequest();
        }
        try {
            long id = Long.parseLong(value);
            if (id > 0) {
                return id;
            }
        } catch (NumberFormatException ignored) {
            // Values outside the signed bigint range are invalid API identifiers.
        }
        throw invalidRequest();
    }

    private String courseName(String value) {
        if (value == null) {
            throw invalidRequest();
        }
        String normalized = value.strip();
        if (normalized.isBlank()) {
            throw invalidRequest();
        }
        checkText(normalized, 100);
        return normalized;
    }

    private String courseMemo(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        checkText(value, 1000);
        return value;
    }

    private void checkText(String value, int maximum) {
        if (value.codePointCount(0, value.length()) > maximum || value.indexOf('\0') >= 0) {
            throw invalidRequest();
        }
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (Character.isHighSurrogate(character)) {
                if (++index >= value.length() || !Character.isLowSurrogate(value.charAt(index))) {
                    throw invalidRequest();
                }
            } else if (Character.isLowSurrogate(character)) {
                throw invalidRequest();
            }
        }
    }

    private BusinessException invalidRequest() {
        return new BusinessException(ErrorCode.INVALID_REQUEST);
    }

    private String validateAndSerializeGeometry(LineStringGeometry geometry) {
        validateGeometry(geometry, maximumCoordinates);
        try {
            return objectMapper.writeValueAsString(geometry);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot serialize course geometry");
        }
    }

    private void validateGeometry(LineStringGeometry geometry, int coordinateLimit) {
        if (geometry == null
                || !"LineString".equals(geometry.type())
                || geometry.coordinates() == null
                || geometry.coordinates().size() < 2
                || geometry.coordinates().size() > coordinateLimit) {
            throw invalidGeometry();
        }
        List<Double> first = geometry.coordinates().get(0);
        boolean distinct = false;
        for (List<Double> point : geometry.coordinates()) {
            if (point == null
                    || point.size() != 2
                    || point.get(0) == null
                    || point.get(1) == null) {
                throw invalidGeometry();
            }
            double longitude = point.get(0);
            double latitude = point.get(1);
            if (!Double.isFinite(longitude)
                    || !Double.isFinite(latitude)
                    || longitude < -180
                    || longitude > 180
                    || latitude < -90
                    || latitude > 90) {
                throw invalidGeometry();
            }
            if (first.get(0) != longitude || first.get(1) != latitude) {
                distinct = true;
            }
        }
        if (!distinct) {
            throw invalidGeometry();
        }
    }

    private BusinessException invalidGeometry() {
        return new BusinessException(ErrorCode.INVALID_COURSE_GEOMETRY);
    }

    private void requireMountain(long mountainId) {
        if (!mapper.activeMountainExists(mountainId)) {
            throw new BusinessException(ErrorCode.MOUNTAIN_NOT_FOUND);
        }
    }

    private CourseRow requireCourse(long courseId, long owner, boolean lock) {
        CourseRow course = mapper.findCourse(courseId, owner, lock);
        if (course == null) {
            throw new BusinessException(ErrorCode.COURSE_NOT_FOUND);
        }
        return course;
    }

    private CourseGeometryRow prepareGeometry(String json, long mountainId, Long courseId) {
        CourseGeometryRow geometry;
        try {
            geometry = mapper.prepareGeometry(json, mountainId, courseId);
        } catch (DataAccessException exception) {
            if (isInputProjectionFailure(exception)) {
                throw new BusinessException(ErrorCode.INVALID_COURSE_GEOMETRY);
            }
            throw exception;
        }
        if (geometry == null) {
            throw stateUnavailable();
        }
        if (!Boolean.TRUE.equals(geometry.areaAvailable())) {
            throw new BusinessException(ErrorCode.COURSE_AREA_UNAVAILABLE);
        }
        if (!Boolean.TRUE.equals(geometry.valid())
                || geometry.geometryHex() == null
                || geometry.lengthM() == null
                || geometry.lengthM().signum() <= 0
                || geometry.lengthM().compareTo(MAXIMUM_LENGTH) > 0) {
            throw new BusinessException(ErrorCode.INVALID_COURSE_GEOMETRY);
        }
        if (!Boolean.TRUE.equals(geometry.covered())) {
            throw new BusinessException(ErrorCode.COURSE_OUTSIDE_SERVICE_AREA);
        }
        return geometry;
    }

    private boolean isInputProjectionFailure(DataAccessException exception) {
        // Only PROJ invalid-coordinate (2049) / outside-projection-domain (2050).
        // Missing SRIDs, grid files, SQL/schema errors and connectivity remain server errors.
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sql && "XX000".equals(sql.getSQLState())) {
                String message = sql.getMessage();
                if (message != null
                        && message.contains("transform:")
                        && (message.contains("(2049)") || message.contains("(2050)"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private CourseDetailResponse detail(CourseRow course) {
        if (course.courseId() == null
                || course.courseId() <= 0
                || course.mountainId() == null
                || course.mountainId() <= 0
                || course.revNo() == null
                || course.revNo() < 1
                || !Boolean.TRUE.equals(course.geometryValid())
                || course.geometryJson() == null
                || course.courseName() == null
                || course.createType() == null
                || course.lengthM() == null
                || course.lengthM().signum() <= 0
                || course.lengthM().compareTo(MAXIMUM_LENGTH) > 0
                || course.createdAt() == null
                || course.updatedAt() == null) {
            throw stateUnavailable();
        }
        LineStringGeometry geometry;
        try {
            geometry = objectMapper.readValue(course.geometryJson(), LineStringGeometry.class);
            validateGeometry(geometry, Integer.MAX_VALUE);
        } catch (JsonProcessingException | BusinessException exception) {
            throw stateUnavailable();
        }
        return new CourseDetailResponse(
                Long.toString(course.courseId()),
                Long.toString(course.mountainId()),
                course.courseName(),
                course.memo(),
                course.createType(),
                course.revNo(),
                geometry,
                course.lengthM(),
                course.createdAt().toInstant(ZoneOffset.UTC).toString(),
                course.updatedAt().toInstant(ZoneOffset.UTC).toString());
    }

    private BusinessException stateUnavailable() {
        return new BusinessException(ErrorCode.COURSE_STATE_UNAVAILABLE);
    }
}
