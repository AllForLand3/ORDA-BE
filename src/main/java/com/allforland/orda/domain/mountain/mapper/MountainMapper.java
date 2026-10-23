package com.allforland.orda.domain.mountain.mapper;

import com.allforland.orda.domain.mountain.dto.MountainDetailResponse;
import com.allforland.orda.domain.mountain.dto.MountainListItem;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MountainMapper {

    List<MountainListItem> findAll(String keyword, int limit, long offset);

    long count(String keyword);

    MountainDetailResponse findById(Long mountainId);
}
