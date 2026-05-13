package com.jaxon.back_end.refresh.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jaxon.back_end.common.entity.BaseEntity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Schema(description = "聊天消息")
@TableName(value = "refresh_token")
public class RefreshToken extends BaseEntity{

    @Schema(description = "用户Id")
    @TableField(value = "user_id")
    private Long userId;

    @Schema(description = "用户类型")
    @TableField(value = "user_type")
    private String userType;

    @Schema(description = "token哈希值")
    @TableField(value = "token_hash")
    private String tokenHash;

    @Schema(description = "生效时间")
    @TableField(value = "expires_at")
    private LocalDateTime expiresAt;

    @Schema(description = "是否已作废")
    @TableField(value = "is_revoked")
    private boolean isRevoked;

    @Schema(description = "作废时间")
    @TableField(value = "revoked_at")
    private LocalDateTime revokedAt;

    @Schema(description = "最后使用时间")
    @TableField(value = "last_used_at")
    private LocalDateTime lastUsedAt;
    
}
