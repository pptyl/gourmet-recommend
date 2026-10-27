package com.gourmet.config;

import com.gourmet.common.redis.RedisService;
import com.gourmet.common.redis.RedissonLockManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 启动时自检 Redis 连通性，并预热 Redisson 分布式锁，避免首位抢券用户为初始化买单。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisStartupChecker implements ApplicationRunner {

    private final RedisService redisService;
    private final RedissonLockManager redissonLockManager;
    private final RedisProperties redisProperties;

    @Override
    public void run(ApplicationArguments args) {
        String host = redisProperties.getHost();
        int port = redisProperties.getPort();
        String user = StringUtils.hasText(redisProperties.getUsername())
                ? redisProperties.getUsername() : "default(单密码)";

        String error = redisService.probe();
        if (error == null) {
            log.info("Redis 连接成功：{}:{}，认证用户={}，秒杀与榜单缓存已启用", host, port, user);
            boolean redissonReady = redissonLockManager.warmUp();
            log.info("Redisson 分布式锁：{}，抢券链路 = 用户锁 + 券级锁 + Lua 原子扣减 + Stream 异步落库",
                    redissonReady ? "已就绪" : "未启用（降级为 Lua + 数据库）");
        } else {
            log.warn("""
                    Redis 不可用（{}:{}，用户={}）：{}
                    优惠券抢购与榜单缓存自动降级为数据库方案。
                    排查顺序：1) 虚拟机 IP 是否可达  2) 是否 bind 0.0.0.0  3) 用户名/密码是否正确（Redis 6+ ACL 需同时给用户名）
                    可用环境变量指定：REDIS_HOST / REDIS_PORT / REDIS_USERNAME / REDIS_PASSWORD""",
                    host, port, user, error);
        }
    }
}
