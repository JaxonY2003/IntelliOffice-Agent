package com.jaxon.back_end.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "发送消息响应对象")
public class SendMessageResponse {

    @Schema(description = "用户消息")
    private ChatMessageDTO userMessage;

    @Schema(description = "Agent回复消息")
    private ChatMessageDTO agentMessage;
}
