package com.jaxon.back_end.chat.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jaxon.back_end.chat.dto.ChatMessageDTO;
import com.jaxon.back_end.chat.entity.ChatMessage;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage>{

    List<ChatMessageDTO> findBySessionId(@Param("sessionId") Long sessionId);
    
}
