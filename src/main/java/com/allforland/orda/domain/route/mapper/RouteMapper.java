package com.allforland.orda.domain.route.mapper;

import com.allforland.orda.domain.route.vo.RoutePathRow;
import com.allforland.orda.domain.route.vo.RouteSnapRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 경로 탐색 조회를 위한 MyBatis 매퍼다. */
@Mapper
public interface RouteMapper {
    boolean activeMountainExists(@Param("mountainId") long mountainId);

    int countMountainNodes(
            @Param("mountainId") long mountainId,
            @Param("startNodeId") long startNodeId,
            @Param("endNodeId") long endNodeId);

    RoutePathRow findShortestPath(
            @Param("startNodeId") long startNodeId, @Param("endNodeId") long endNodeId);

    RouteSnapRow findNearestNode(
            @Param("mountainId") long mountainId,
            @Param("longitude") double longitude,
            @Param("latitude") double latitude,
            @Param("maxDistanceM") double maxDistanceM);
}
