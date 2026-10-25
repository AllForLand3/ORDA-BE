package com.allforland.orda.domain.user.mapper;

import com.allforland.orda.domain.user.vo.UserAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {
    UserAccount findByLoginId(@Param("loginId") String loginId);

    UserAccount findByUserId(@Param("userId") long userId);

    Long insertUser(
            @Param("loginId") String loginId,
            @Param("userNm") String userNm,
            @Param("passwordHash") String passwordHash);
}
