package com.gourmet.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 普通用户。
 */
@Data
@TableName("sys_user")
@Schema(description = "普通用户")
public class User implements Serializable {

    @TableId(type = IdType.AUTO)
    @Schema(description = "用户 ID")
    private Long id;

    @Schema(description = "手机号（登录账号）")
    private String phone;

    @JsonIgnore
    private String password;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像地址")
    private String avatar;

    @Schema(description = "联系电话")
    private String contactPhone;

    @Schema(description = "偏好菜系，逗号分隔")
    private String tasteCuisine;

    @Schema(description = "口味轻重：清淡/微辣/中辣/重辣")
    private String tasteFlavor;

    @Schema(description = "忌口食材，逗号分隔")
    private String taboo;

    @Schema(description = "1 正常 0 禁用")
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @JsonIgnore
    @TableLogic
    private Integer deleted;
}
