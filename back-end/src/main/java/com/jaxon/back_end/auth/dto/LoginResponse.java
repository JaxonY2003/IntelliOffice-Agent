package com.jaxon.back_end.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "登录响应对象")
public class LoginResponse {
    
    @Schema(description = "token")
    private String token;
    @Schema(description = "token类型")
    private String tokenType = "Bearer";
    @Schema(description = "用户名")
    private String username;
    @Schema(description = "角色类型")
    private String type;
}
