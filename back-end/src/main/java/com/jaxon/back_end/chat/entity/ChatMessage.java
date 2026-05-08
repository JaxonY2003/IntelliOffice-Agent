package com.jaxon.back_end.chat.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jaxon.back_end.common.entity.BaseEntity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "聊天消息")
@TableName(value = "chat_message")
public class ChatMessage extends BaseEntity{
    
    @Schema(description = "会话ID")
    @TableField(value = "session_id")
    private Long sessionId;

    @Schema(description = "发送发类型(USER/System/Agent)")
    @TableField(value = "sender_type")
    private String senderType;

    @Schema(description = "消息类型(文本、图像、文档等)")
    @TableField(value = "message_type")
    private String messageType;

    @Schema(description = "消息内容")
    @TableField(value = "content")
    private String content;
}
