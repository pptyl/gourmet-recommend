package com.gourmet.common.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

/**
 * Redisson 分布式锁管理。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedissonLockManager {

    private final RedisProperties redisProperties;

    private volatile RedissonClient client;
    private volatile boolean initFailed;

    private RedissonClient client() {
        if (client == null && !initFailed) {
            synchronized (this) {
                if (client == null && !initFailed) {
                    try {
                        client = Redisson.create(buildConfig());
                        log.info("Redisson 初始化成功：{}:{}", redisProperties.getHost(), redisProperties.getPort());
                    } catch (Exception e) {
                        initFailed = true;
                        log.error("Redisson 初始化失败，抢券将降级为 Lua/数据库方案：{}", e.getMessage());
                    }
                }
            }
        }
        return client;
    }

    private Config buildConfig() {
        Config config = new Config();
        SingleServerConfig server = config.useSingleServer()
                .setAddress("redis://" + redisProperties.getHost() + ":" + redisProperties.getPort())
                .setDatabase(redisProperties.getDatabase())
                .setConnectTimeout(3000)
                .setTimeout(3000)
                .setRetryAttempts(2)
                .setConnectionMinimumIdleSize(4)
                .setConnectionPoolSize(16);
        if (StringUtils.hasText(redisProperties.getUsername())) {
            server.setUsername(redisProperties.getUsername());
        }
        if (StringUtils.hasText(redisProperties.getPassword())) {
            server.setPassword(redisProperties.getPassword());
        }
        return config;
    }

    /**
     * 尝试加锁。
     *
     * @param waitMs  等待时间（0 表示不等待，立即失败）
     * @param leaseMs 锁租期
     */
    public boolean tryLock(String key, long waitMs, long leaseMs) {
        RedissonClient c = client();
        if (c == null) {
            return false;
        }
        RLock lock = c.getLock(key);
        try {
            return lock.tryLock(waitMs, leaseMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            log.warn("Redisson 加锁失败 {}：{}", key, e.getMessage());
            return false;
        }
    }

    /**
     * 释放锁。
     */
    public void unlock(String key) {
        RedissonClient c = client();
        if (c == null) {
            return;
        }
        try {
            RLock lock = c.getLock(key);
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        } catch (Exception e) {
            log.debug("释放锁失败 {}：{}", key, e.getMessage());
        }
    }

    public boolean available() {
        return client() != null;
    }

    /**
     * 启动预热，提前建立连接。
     */
    public boolean warmUp() {
        return client() != null;
    }
}
