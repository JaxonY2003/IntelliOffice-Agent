package com.jaxon.back_end.refresh.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jaxon.back_end.refresh.entity.RefreshToken;

@Mapper
public interface RefreshTokenMapper extends BaseMapper<RefreshToken>{

    RefreshToken findByUserIdAndUserType(
        @Param("userId") Long userId,
        @Param("userType") String userType);

    RefreshToken findByRefreshHash(@Param("refreshHash") String refreshHash);

    int insertRefreshToken(@Param("refreshToken") RefreshToken refreshToken);

    int updateRefreshToken(@Param("refreshToken") RefreshToken refreshToken);

    int revokeRefreshTokenByTokenHash(@Param("tokenHash") String tokenHash);

    
    
}
