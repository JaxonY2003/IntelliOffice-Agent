package com.jaxon.back_end.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "刷新token返回对象")
@Data
public class RefreshTokenResponse {
    
    @Schema(description = "accessToken")
    private String token;

    @Schema(description = "token类型")
    private String tokenType = "Bearer";
    
    @Schema(description = "refreshToken")
    private String refreshToken;
}
