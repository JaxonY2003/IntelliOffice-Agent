package com.jaxon.back_end.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Python agent 回复对象")
public class AgentReplyResponse {

    @Schema(description = "回复内容")
    private String content;
}
