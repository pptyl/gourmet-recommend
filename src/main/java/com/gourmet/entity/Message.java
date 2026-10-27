package com.gourmet.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 站内消息。
 */
@Data
@TableName("message")
@Schema(description = "消息")
public class Message implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "接收者 ID")
    private Long receiverId;

    @Schema(description = "1 用户 2 商家")
    private Integer receiverType;

    @Schema(description = "1 新店开业 2 新品上架 3 优惠券 4 系统 5 审核结果")
    private Integer type;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "内容")
    private String content;

    @Schema(description = "关联业务 ID")
    private Long bizId;

    @Schema(description = "0 未读 1 已读")
    private Integer isRead;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
