package com.gourmet.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 平台管理员。
 */
@Data
@TableName("sys_admin")
@Schema(description = "平台管理员")
public class Admin implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "登录用户名")
    private String username;

    @JsonIgnore
    private String password;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "1 正常 0 禁用")
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
