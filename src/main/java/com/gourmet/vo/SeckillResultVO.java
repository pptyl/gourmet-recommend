package com.gourmet.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 抢券结果。
 */
@Data
@Schema(description = "抢券结果")
public class SeckillResultVO implements Serializable {

    @Schema(description = "优惠券 ID")
    private Long couponId;

    @Schema(description = "请求号（用于查询处理状态）")
    private String requestId;

    @Schema(description = "是否已进入异步队列（true 表示已受理、正在落库）")
    private boolean queued;

    @Schema(description = "状态：QUEUED 排队中 / DONE 已入券包 / REJECTED 已拒绝")
    private String status;

    @Schema(description = "提示文案")
    private String message;

    public static SeckillResultVO done(Long couponId, String requestId, String message) {
        SeckillResultVO vo = new SeckillResultVO();
        vo.setCouponId(couponId);
        vo.setRequestId(requestId);
        vo.setQueued(false);
        vo.setStatus("DONE");
        vo.setMessage(message);
        return vo;
    }

    public static SeckillResultVO queued(Long couponId, String requestId, String message) {
        SeckillResultVO vo = new SeckillResultVO();
        vo.setCouponId(couponId);
        vo.setRequestId(requestId);
        vo.setQueued(true);
        vo.setStatus("QUEUED");
        vo.setMessage(message);
        return vo;
    }
}
