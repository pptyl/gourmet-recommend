package com.gourmet.service;

import com.gourmet.vo.SeckillResultVO;

/**
 * 抢券服务。
 */
public interface CouponSeckillService {

    /** 抢券 */
    SeckillResultVO seckill(Long userId, Long couponId);

    /** 查询抢券处理状态 */
    SeckillResultVO orderStatus(Long userId, Long couponId);

    /** 订单落库 */
    void handleOrder(String requestId, Long userId, Long couponId, long receiveTime);
}
