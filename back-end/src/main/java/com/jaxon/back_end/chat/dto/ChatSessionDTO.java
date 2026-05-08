package com.jaxon.back_end.chat.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "查询用户聊天对话返回对象")
public class ChatSessionDTO {
    
    @Schema(description = "id")
    private Long id;

    @Schema(description = "title")
    private String title;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
