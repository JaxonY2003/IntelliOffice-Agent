package com.jaxon.back_end.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "登出请求对象")
@Data
public class LogoutRequest {
    
    @Schema(description = "refreshToken")
    private String refreshToken;
}
