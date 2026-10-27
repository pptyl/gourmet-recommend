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
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商家（入驻申请复用同一张表：auditStatus = 0 即待审核）。
 */
@Data
@TableName("merchant")
@Schema(description = "商家")
public class Merchant implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "店铺名称")
    private String name;

    @Schema(description = "登录手机号")
    private String phone;

    @JsonIgnore
    private String password;

    @Schema(description = "菜系")
    private String category;

    @Schema(description = "所属地区")
    private String area;

    @Schema(description = "详细地址")
    private String address;

    @Schema(description = "店铺简介")
    private String intro;

    @Schema(description = "店铺封面")
    private String coverImage;

    @Schema(description = "店铺相册，逗号分隔")
    private String album;

    @Schema(description = "营业时间")
    private String businessHours;

    @Schema(description = "人均消费")
    private BigDecimal avgPrice;

    @Schema(description = "综合评分")
    private BigDecimal score;

    @Schema(description = "评分人数")
    private Integer scoreCount;

    @Schema(description = "收藏量")
    private Integer favoriteCount;

    @Schema(description = "评价数量")
    private Integer reviewCount;

    @Schema(description = "浏览量")
    private Integer viewCount;

    @Schema(description = "1 正常营业 0 停业整顿")
    private Integer status;

    @Schema(description = "0 待审核 1 已通过 2 已驳回")
    private Integer auditStatus;

    @Schema(description = "审核意见")
    private String auditRemark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @JsonIgnore
    @TableLogic
    private Integer deleted;

    /* ---------------- 以下为推荐场景的展示字段，不落库 ---------------- */

    @TableField(exist = false)
    @Schema(description = "推荐理由")
    private String recommendReason;

    @TableField(exist = false)
    @Schema(description = "推荐算法：PREFERENCE/HOT/SIMILAR/COLD_START/RANDOM")
    private String recommendAlgorithm;

    @TableField(exist = false)
    @Schema(description = "推荐得分（0-100）")
    private Double recommendScore;

    @TableField(exist = false)
    @Schema(description = "热度值")
    private Double heat;

    @TableField(exist = false)
    @Schema(description = "当前用户是否已收藏")
    private Boolean favorite;
}
