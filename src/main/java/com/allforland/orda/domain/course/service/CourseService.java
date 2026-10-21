package com.allforland.orda.domain.course.service;

import com.allforland.orda.common.error.BusinessException;
import com.allforland.orda.common.error.ErrorCode;
import com.allforland.orda.common.geo.LineStringGeometry;
import com.allforland.orda.domain.course.dto.CourseCreateRequest;
import com.allforland.orda.domain.course.dto.CourseDetailResponse;
import com.allforland.orda.domain.course.dto.CourseUpdateRequest;
import com.allforland.orda.domain.course.mapper.CourseMapper;
import com.allforland.orda.domain.course.vo.CourseGeometryRow;
import com.allforland.orda.domain.course.vo.CourseRow;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 코스 CRUD의 검증과 업무 규칙을 처리한다. */
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

    /** 코스 입력을 검증하고 새 코스를 저장한다. */
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
                        geometry.lengthM());
        if (courseId == null || courseId <= 0) {
            throw stateUnavailable();
        }
        return detail(requireCourse(courseId, owner, false));
    }

    /** 작업공간에서 편집 가능한 코스 상세 정보를 조회한다. */
    @Transactional(readOnly = true)
    public CourseDetailResponse get(String courseId) {
        long id = positiveId(courseId);
        long owner = requireOwner();
        return detail(requireCourse(id, owner, false));
    }

    /** If-Match를 확인하고 코스의 허용된 필드를 수정한다. */
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
            // API 좌표가 같으면 좌표계 왕복 변환 오차로 형상 버전을 올리지 않는다.
            boolean sameApiGeometry =
                    current.geometry().coordinates().equals(request.geometry().coordinates());
            geometryChanged = !sameApiGeometry && !Boolean.TRUE.equals(geometry.sameGeometry());
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
                        geometryChanged ? geometry.lengthM() : null);
        requireSingleChange(affected);
        return detail(requireCourse(id, owner, false));
    }

    /** If-Match를 확인하고 코스를 논리 삭제한다. */
    @Transactional
    public void delete(String courseId, String ifMatch) {
        long id = positiveId(courseId);
        String expected = editTags.requireIfMatch(ifMatch);
        long owner = requireOwner();
        CourseDetailResponse current = detail(requireCourse(id, owner, true));
        editTags.verify(expected, current);
        requireSingleChange(mapper.deleteCourse(id, owner));
    }

    /** Mapper 변경 건수가 한 건인지 확인한다. */
    private void requireSingleChange(int affected) {
        if (affected != 1) {
            throw stateUnavailable();
        }
    }

    /** 설정된 활성 시연 계정을 코스 작업공간 소유자로 확인한다. */
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

    /** 양의 long 범위의 10진 식별자를 검증한다. */
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

    /** 코스명을 정규화하고 길이와 내용을 검증한다. */
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

    /** 코스 메모를 정규화하고 길이를 검증한다. */
    private String courseMemo(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        checkText(value, 1000);
        return value;
    }

    /** 텍스트 길이와 잘못된 유니코드 문자를 검증한다. */
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

    /** 일반 요청 오류를 생성한다. */
    private BusinessException invalidRequest() {
        return new BusinessException(ErrorCode.INVALID_REQUEST);
    }

    /** 코스 형상을 검증한 뒤 JSON 문자열로 직렬화한다. */
    private String validateAndSerializeGeometry(LineStringGeometry geometry) {
        validateGeometry(geometry, maximumCoordinates);
        try {
            return objectMapper.writeValueAsString(geometry);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot serialize course geometry");
        }
    }

    /** LineString 좌표의 형식, 범위, 개수를 검증한다. */
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

    /** 코스 형상 오류를 생성한다. */
    private BusinessException invalidGeometry() {
        return new BusinessException(ErrorCode.INVALID_COURSE_GEOMETRY);
    }

    /** 산이 활성 상태인지 확인한다. */
    private void requireMountain(long mountainId) {
        if (!mapper.activeMountainExists(mountainId)) {
            throw new BusinessException(ErrorCode.MOUNTAIN_NOT_FOUND);
        }
    }

    /** 작업공간 소유 범위에서 코스를 조회하고 필요하면 행을 잠근다. */
    private CourseRow requireCourse(long courseId, long owner, boolean lock) {
        CourseRow course = mapper.findCourse(courseId, owner, lock);
        if (course == null) {
            throw new BusinessException(ErrorCode.COURSE_NOT_FOUND);
        }
        return course;
    }

    /** DB에서 형상을 변환·검증하고 길이와 서비스 영역 포함 여부를 확인한다. */
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

    /** 사용자 좌표에 의한 투영 오류인지 구분한다. */
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

    /** DB 코스 행을 검증해 상세 응답으로 변환한다. */
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
                || course.rowVersion() == null
                || course.rowVersion().isBlank()) {
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
                course.rowVersion());
    }

    /** 코스 저장 상태 오류를 생성한다. */
    private BusinessException stateUnavailable() {
        return new BusinessException(ErrorCode.COURSE_STATE_UNAVAILABLE);
    }
}
