package com.jaxon.back_end.chat.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "调用 Python agent 的请求对象")
public class AgentReplyRequest {

    @Schema(description = "会话ID")
    private Long sessionId;

    @Schema(description = "消息上下文")
    private List<AgentChatMessage> messages;
}
