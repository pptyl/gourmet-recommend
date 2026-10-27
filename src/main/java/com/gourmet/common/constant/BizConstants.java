package com.gourmet.common.constant;

/**
 * 全局业务常量。
 */
public final class BizConstants {

    private BizConstants() {
    }

    /* ---------------- 角色 ---------------- */
    public static final String ROLE_USER = "USER";
    public static final String ROLE_MERCHANT = "MERCHANT";
    public static final String ROLE_ADMIN = "ADMIN";

    /* ---------------- 通用状态 ---------------- */
    public static final int ENABLED = 1;
    public static final int DISABLED = 0;

    /* ---------------- 商家审核状态 ---------------- */
    public static final int MERCHANT_AUDIT_PENDING = 0;
    public static final int MERCHANT_AUDIT_PASSED = 1;
    public static final int MERCHANT_AUDIT_REJECTED = 2;

    /* ---------------- 商家营业状态 ---------------- */
    public static final int MERCHANT_OPEN = 1;
    public static final int MERCHANT_CLOSED = 0;

    /* ---------------- 菜品上下架 ---------------- */
    public static final int DISH_ON_SALE = 1;
    public static final int DISH_OFF_SALE = 0;

    /* ---------------- 评价 / 笔记审核状态 ---------------- */
    public static final int CONTENT_PENDING = 0;
    public static final int CONTENT_APPROVED = 1;
    public static final int CONTENT_REJECTED = 2;

    /* ---------------- 优惠券状态 ---------------- */
    public static final int COUPON_ON = 1;
    public static final int COUPON_OFF = 0;

    /* ---------------- 用户券包状态 ---------------- */
    public static final int USER_COUPON_UNUSED = 0;
    public static final int USER_COUPON_USED = 1;
    public static final int USER_COUPON_EXPIRED = 2;

    /* ---------------- 推荐反馈 ---------------- */
    public static final int FEEDBACK_LIKE = 1;
    public static final int FEEDBACK_DISLIKE = 2;

    /* ---------------- 推荐来源 ---------------- */
    public static final String SOURCE_HOME = "HOME";
    public static final String SOURCE_SIMILAR = "SIMILAR";
    public static final String SOURCE_RANK = "RANK";

    /* ---------------- 推荐算法标识 ---------------- */
    public static final String ALGO_PREFERENCE = "PREFERENCE";
    public static final String ALGO_HOT = "HOT";
    public static final String ALGO_SIMILAR = "SIMILAR";
    public static final String ALGO_COLD_START = "COLD_START";
    public static final String ALGO_RANDOM = "RANDOM";

    /* ---------------- 消息类型 ---------------- */
    public static final int MSG_NEW_SHOP = 1;
    public static final int MSG_NEW_DISH = 2;
    public static final int MSG_COUPON = 3;
    public static final int MSG_SYSTEM = 4;
    public static final int MSG_AUDIT = 5;

    /* ---------------- 消息接收者类型 ---------------- */
    public static final int RECEIVER_USER = 1;
    public static final int RECEIVER_MERCHANT = 2;

    /* ---------------- 秒杀结果码（与 Lua 脚本一致） ---------------- */
    public static final long SECKILL_SUCCESS = 1L;
    public static final long SECKILL_NO_STOCK = -1L;
    public static final long SECKILL_DUPLICATE = -2L;
    public static final long SECKILL_NOT_INIT = -3L;

    /* ---------------- 分页保护 ---------------- */
    public static final long MAX_PAGE_SIZE = 100L;
}
