package com.jaxon.back_end.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "发送消息请求对象")
public class SendMessageRequest {

    @Schema(description = "会话ID")
    private Long sessionId;

    @Schema(description = "消息类型(文本、图像、文档等)")
    private String messageType;

    @Schema(description = "消息内容")
    private String content;
}
