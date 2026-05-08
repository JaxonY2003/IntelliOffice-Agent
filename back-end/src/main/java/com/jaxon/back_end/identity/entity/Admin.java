package com.jaxon.back_end.identity.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jaxon.back_end.common.entity.BaseEntity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.ToString;

@Data
@Schema(description = "管理员")
@TableName(value = "admin")
public class Admin extends BaseEntity{
    private static final long serialVersionUID = 1L;

    @Schema(description = "用户名")
    @TableField(value = "username")
    private String username;

    @Schema(description = "密码")
    @TableField(value = "password")
    @JsonIgnore
    @ToString.Exclude
    private String password;

    @Schema(description = "部门")
    @TableField(value = "department")
    private String department;
}
