package com.allforland.orda.domain.analysis.terrain.service;

import com.allforland.orda.common.error.BusinessException;
import com.allforland.orda.common.error.ErrorCode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/** 실제 코스 길이에서 시작점·끝점을 포함한 샘플 거리 목록을 생성한다. */
@Component
public class CourseSampleDistanceGenerator {

    private static final BigDecimal DEFAULT_INTERVAL_M = new BigDecimal("20.0");

    private static final BigDecimal MIN_INTERVAL_M = new BigDecimal("5.0");

    private static final BigDecimal MAX_INTERVAL_M = new BigDecimal("100.0");

    private static final int MAX_SAMPLE_COUNT = 2000;

    // 끝점과 내부 샘플의 수치 오차 판정용이다. 저장 정밀도와는 별개다.
    private static final double ENDPOINT_TOLERANCE_M = 1e-7;

    /** 생략한 간격은 20m를 사용한다. 허용 범위와 정밀도를 검사하고 소수점 한 자리로 표현을 통일한다. */
    public BigDecimal normalizeInterval(BigDecimal requestedIntervalM) {
        BigDecimal interval = requestedIntervalM == null ? DEFAULT_INTERVAL_M : requestedIntervalM;

        if (interval.compareTo(MIN_INTERVAL_M) < 0
                || interval.compareTo(MAX_INTERVAL_M) > 0
                || interval.stripTrailingZeros().scale() > 1) {
            throw new BusinessException(ErrorCode.ANALYSIS_INVALID_SAMPLE_INTERVAL);
        }

        // 20, 20.0, 20.00은 모두 20.0으로 표현한다.
        // 20.01처럼 실제 정밀도가 초과한 값은 위에서 거부한다.
        return interval.setScale(1, RoundingMode.UNNECESSARY);
    }

    /** 반올림하지 않은 EPSG:5179 형상 길이로 샘플 거리를 생성한다. 반환 목록은 수정할 수 없으며 첫 값은 0, 마지막 값은 실제 길이다. */
    public List<Double> generate(double actualLengthM, BigDecimal requestedIntervalM) {

        BigDecimal interval = normalizeInterval(requestedIntervalM);

        validateLength(actualLengthM);

        double intervalM = interval.doubleValue();

        // 시작점·끝점을 포함해 최대 2,000개를 만들 수 있는 길이인지
        // 목록을 생성하기 전에 검사한다.
        double maximumLengthM = intervalM * (MAX_SAMPLE_COUNT - 1);

        if (actualLengthM > maximumLengthM + ENDPOINT_TOLERANCE_M) {
            throw new BusinessException(ErrorCode.ANALYSIS_SAMPLE_LIMIT_EXCEEDED);
        }

        int interiorCount = (int) Math.floor(actualLengthM / intervalM);

        if (interiorCount > 0) {
            double lastRegularDistance =
                    interval.multiply(BigDecimal.valueOf(interiorCount)).doubleValue();

            // 끝점에 해당하거나 수치 오차 범위 안이면
            // 내부 샘플에서 제외하고 실제 끝점을 한 번만 추가한다.
            if (actualLengthM - lastRegularDistance <= ENDPOINT_TOLERANCE_M) {
                interiorCount--;
            }
        }

        int sampleCount = interiorCount + 2;

        if (sampleCount > MAX_SAMPLE_COUNT) {
            throw new BusinessException(ErrorCode.ANALYSIS_SAMPLE_LIMIT_EXCEEDED);
        }

        List<Double> distances = new ArrayList<>(sampleCount);

        distances.add(0.0);

        for (int index = 1; index <= interiorCount; index++) {
            // 반복 덧셈 대신 간격 × 순번으로 계산한다.
            double distanceM = interval.multiply(BigDecimal.valueOf(index)).doubleValue();

            distances.add(distanceM);
        }

        distances.add(actualLengthM);

        return List.copyOf(distances);
    }

    /** DB 형상에서 계산한 실제 길이가 유한한 양수인지 확인한다. */
    private void validateLength(double actualLengthM) {
        if (!Double.isFinite(actualLengthM) || actualLengthM <= 0.0) {
            throw new BusinessException(ErrorCode.COURSE_STATE_UNAVAILABLE);
        }
    }
}
