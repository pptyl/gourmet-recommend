package com.gourmet.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 平台整体业务数据。
 */
@Data
@Schema(description = "平台业务总览")
public class AdminOverviewVO implements Serializable {

    @Schema(description = "普通用户总数")
    private long userCount;

    @Schema(description = "入驻商家总数（审核通过）")
    private long merchantCount;

    @Schema(description = "待审核商家数")
    private long pendingMerchantCount;

    @Schema(description = "评价总数")
    private long reviewCount;

    @Schema(description = "待审核评价数")
    private long pendingReviewCount;

    @Schema(description = "探店笔记总数")
    private long noteCount;

    @Schema(description = "待审核笔记数")
    private long pendingNoteCount;

    @Schema(description = "优惠券活动数")
    private long couponCount;

    @Schema(description = "优惠券领取总数")
    private long couponClaimCount;

    @Schema(description = "今日新增用户")
    private long todayUserCount;

    @Schema(description = "今日新增评价")
    private long todayReviewCount;
}
