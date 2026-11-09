package com.allforland.orda.domain.trail.mapper;

import com.allforland.orda.domain.trail.vo.FacilityDetailRow;
import com.allforland.orda.domain.trail.vo.NearbyFacilityRow;
import com.allforland.orda.domain.trail.vo.NearbySpotRow;
import com.allforland.orda.domain.trail.vo.SpotDetailRow;
import com.allforland.orda.domain.trail.vo.SpotMatchRow;
import com.allforland.orda.domain.trail.vo.SpotTagRow;
import com.allforland.orda.domain.trail.vo.TrailCourseRow;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 상세지점·시설 조회를 위한 MyBatis 매퍼다. */
@Mapper
public interface TrailMapper {

    /** 삭제되지 않은 소유자 코스를 조회한다. 없거나 다른 사용자 소유이면 null을 반환한다. */
    TrailCourseRow findOwnedCourse(
            @Param("courseId") long courseId, @Param("ownerUserId") long ownerUserId);

    /** 산에 조회 가능한 태그 상세지점 자료가 있는지 확인한다. */
    boolean spotDataExists(@Param("mountainId") long mountainId);

    /** 코스 반경 안에서 조회 가능한 태그를 가진 대표 상세지점을 거리순으로 조회한다. */
    List<NearbySpotRow> findNearbySpots(
            @Param("courseId") long courseId,
            @Param("radiusM") int radiusM,
            @Param("tag") String tag,
            @Param("verifyStatus") String verifyStatus);

    /** 상세지점들의 태그를 조회한다. includeRejected가 false이면 REJECTED 태그를 제외한다. */
    List<SpotTagRow> findSpotTags(
            @Param("spotIds") List<Long> spotIds,
            @Param("includeRejected") boolean includeRejected,
            @Param("verifyStatus") String verifyStatus);

    /** 산에 조회 가능한 시설 자료가 있는지 확인한다. */
    boolean facilityDataExists(@Param("mountainId") long mountainId, @Param("type") String type);

    /** 코스 반경 안의 대표 시설을 거리순으로 조회한다. */
    List<NearbyFacilityRow> findNearbyFacilities(
            @Param("courseId") long courseId,
            @Param("radiusM") int radiusM,
            @Param("type") String type);

    /** 사용 중인 산인지 확인한다. */
    boolean activeMountainExists(@Param("mountainId") long mountainId);

    /** 요청 간선 중 해당 산에 속한 통행 가능 간선 수를 센다. */
    int countRouteEdges(@Param("mountainId") long mountainId, @Param("edgeIds") List<Long> edgeIds);

    /** 후보 간선 반경 안에서 조회 가능한 태그를 가진 대표 상세지점을 거리순으로 조회한다. */
    List<NearbySpotRow> findRouteNearbySpots(
            @Param("mountainId") long mountainId,
            @Param("edgeIds") List<Long> edgeIds,
            @Param("radiusM") int radiusM);

    /** 후보 간선 반경 안의 대표 시설을 거리순으로 조회한다. */
    List<NearbyFacilityRow> findRouteNearbyFacilities(
            @Param("mountainId") long mountainId,
            @Param("edgeIds") List<Long> edgeIds,
            @Param("radiusM") int radiusM);

    SpotDetailRow findSpot(@Param("spotId") long spotId);

    List<SpotMatchRow> findSpotMatches(@Param("spotId") long spotId);

    FacilityDetailRow findFacility(@Param("facilityId") long facilityId);
}
