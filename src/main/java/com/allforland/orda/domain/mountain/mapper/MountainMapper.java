package com.allforland.orda.domain.mountain.mapper;

import com.allforland.orda.domain.mountain.dto.MountainDetailResponse;
import com.allforland.orda.domain.mountain.dto.MountainListItem;
import com.allforland.orda.domain.mountain.dto.MountainSearchRequest;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MountainMapper {

    List<MountainListItem> findAll(
            @Param("request") MountainSearchRequest request, int limit, long offset);

    long count(@Param("request") MountainSearchRequest request);

    MountainDetailResponse findById(Long mountainId);
}
