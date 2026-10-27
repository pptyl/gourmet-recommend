package com.gourmet.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 秒杀与缓存配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.seckill")
public class SeckillProperties {

    /** 是否启用 Redis 秒杀 */
    private boolean redisEnabled = true;

    /** 库存 key 前缀 */
    private String stockKeyPrefix = "seckill:stock:";

    /** 已抢购用户集合 key 前缀 */
    private String userKeyPrefix = "seckill:users:";

    /** 热门榜单缓存 key */
    private String rankCacheKey = "cache:hot-rank";

    /** 榜单缓存秒数 */
    private long rankCacheSeconds = 300;

    /** 是否启用异步下单 */
    private boolean asyncEnabled = true;

    /** 订单消息 Stream key */
    private String streamKey = "stream:coupon:order";

    /** 消费者组 */
    private String consumerGroup = "cg:coupon-order";

    /** 消费者名称 */
    private String consumerName = "coupon-consumer-1";

    /** 单批拉取数量 */
    private int batchSize = 500;

    /** 用户级锁等待时间（毫秒） */
    private long userLockWaitMs = 0;

    /** 锁租期（毫秒） */
    private long lockLeaseMs = 3000;

    /** 是否启用用户级幂等锁 */
    private boolean userLockEnabled = false;

    /** 优惠券本地缓存秒数 */
    private long couponCacheSeconds = 3;

    /** Redis 可用性探测缓存毫秒数 */
    private long redisCheckIntervalMs = 2000;

    public String stockKey(Long couponId) {
        return stockKeyPrefix + couponId;
    }

    public String userKey(Long couponId) {
        return userKeyPrefix + couponId;
    }

    /** 用户级幂等锁 key */
    public String userLockKey(Long couponId, Long userId) {
        return "lock:seckill:user:" + couponId + ":" + userId;
    }

    /** 券级库存锁 key */
    public String stockLockKey(Long couponId) {
        return "lock:seckill:stock:" + couponId;
    }
}
