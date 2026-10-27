package com.gourmet.common.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * 基于 Redis 的分布式锁（SET NX PX + Lua 安全释放）。
 *
 * <p>典型用途：缓存重建防击穿、库存初始化防并发覆盖、任务幂等。
 * Redis 不可用时 tryLock 返回 null，调用方按"未获得锁"降级处理。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisLockUtil {

    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 尝试加锁。
     *
     * @return 锁标识（释放时需要），加锁失败或 Redis 不可用返回 null
     */
    public String tryLock(String key, Duration ttl) {
        String token = UUID.randomUUID().toString().replace("-", "");
        try {
            Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(key, token, ttl);
            return Boolean.TRUE.equals(success) ? token : null;
        } catch (Exception e) {
            log.warn("获取分布式锁失败，key={}：{}", key, e.getMessage());
            return null;
        }
    }

    /**
     * 释放锁：只有持有者才能删除，避免误删他人锁。
     */
    public void unlock(String key, String token) {
        if (token == null) {
            return;
        }
        try {
            stringRedisTemplate.execute(UNLOCK_SCRIPT, List.of(key), token);
        } catch (Exception e) {
            log.warn("释放分布式锁失败，key={}：{}", key, e.getMessage());
        }
    }
}
