package com.jaxon.back_end.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Python agent 消息对象")
public class AgentChatMessage {

    @Schema(description = "消息角色(system/user/assistant)")
    private String role;

    @Schema(description = "消息内容")
    private String content;
}
