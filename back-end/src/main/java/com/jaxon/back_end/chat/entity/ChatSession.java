package com.jaxon.back_end.chat.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jaxon.back_end.common.entity.BaseEntity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "聊天会话")
@TableName(value = "chat_session")
public class ChatSession extends BaseEntity{
    
    @Schema(description = "用户id")
    @TableField(value = "user_id")
    private Long userId;

    @Schema(description = "用户类型")
    @TableField(value = "user_type")
    private String userType;

    @Schema(description = "标题")
    @TableField(value = "title")
    private String title;
}
