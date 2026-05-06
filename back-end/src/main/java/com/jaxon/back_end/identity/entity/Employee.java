package com.jaxon.back_end.identity.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jaxon.back_end.common.entity.BaseEntity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "员工")
@TableName(value = "employee")
public class Employee extends BaseEntity{
    private static final long serialVersionUID = 1L;

    @Schema(description = "用户名")
    @TableField(value = "username")
    private String username;

    @Schema(description = "密码")
    @TableField(value = "password")
    @JsonIgnore
    private String password;

    @Schema(description = "部门")
    @TableField(value = "department")
    private String department;
}
