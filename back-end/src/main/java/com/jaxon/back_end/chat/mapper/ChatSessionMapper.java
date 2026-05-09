package com.jaxon.back_end.chat.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jaxon.back_end.chat.dto.ChatSessionDTO;
import com.jaxon.back_end.chat.entity.ChatSession;

@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSession>{
    
    List<ChatSessionDTO> findByUserIdAndUserType(
            @Param("userId") Long userId,
            @Param("userType") String userType);

    int insertNewSession(@Param("chatSession") ChatSession chatSession);

    void deleteSession(@Param("sessionId") Long sessionId);

    void resetTitle(@Param("newTitle") String newTitle, @Param("sessionId") Long sessionId);
}
