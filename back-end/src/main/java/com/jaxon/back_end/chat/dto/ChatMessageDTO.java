package com.jaxon.back_end.chat.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "聊天会话消息对象")
public class ChatMessageDTO {

    @Schema(description = "id")
    private Long id;

    @Schema(description = "发送者身份(USER/SYSTEM/AGENT)")
    private String senderType;

    @Schema(description = "消息内容")
    private String content;

    @Schema(description = "消息类型(文本、图像、文档)")
    private String messageType;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    
}
