package com.gourmet.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 个人数据统计。
 */
@Data
@Schema(description = "个人数据统计")
public class UserStatisticVO implements Serializable {

    @Schema(description = "评价数量")
    private long reviewCount;

    @Schema(description = "探店笔记数量")
    private long noteCount;

    @Schema(description = "收藏店铺数量")
    private long favoriteCount;

    @Schema(description = "心愿单数量")
    private long wishCount;

    @Schema(description = "领取优惠券总数")
    private long couponCount;

    @Schema(description = "未使用优惠券数量")
    private long unusedCouponCount;

    @Schema(description = "浏览过的店铺数")
    private long browseCount;

    @Schema(description = "未读消息数")
    private long unreadMessageCount;
}
