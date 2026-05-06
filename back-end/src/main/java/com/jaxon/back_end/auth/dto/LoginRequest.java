package com.jaxon.back_end.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "登录请求对象")
@Data
public class LoginRequest {

    @Schema(description = "用户名")
    private String username;
    @Schema(description = "密码")
    private String password;
    @Schema(description = "角色类型")
    private String type;
    
}
