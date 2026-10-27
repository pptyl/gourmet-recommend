package com.gourmet.common.redis;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * Redis 操作封装。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisService {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public String get(String key) {
        try {
            return stringRedisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.warn("Redis 读取失败，key={}，已降级：{}", key, e.getMessage());
            return null;
        }
    }

    public void set(String key, String value, Duration ttl) {
        try {
            if (ttl == null) {
                stringRedisTemplate.opsForValue().set(key, value);
            } else {
                stringRedisTemplate.opsForValue().set(key, value, ttl);
            }
        } catch (Exception e) {
            log.warn("Redis 写入失败，key={}，已降级：{}", key, e.getMessage());
        }
    }

    public void set(String key, String value) {
        set(key, value, null);
    }

    public boolean setIfAbsent(String key, String value, Duration ttl) {
        try {
            Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(key, value, ttl);
            return Boolean.TRUE.equals(success);
        } catch (Exception e) {
            log.warn("Redis setIfAbsent 失败，key={}：{}", key, e.getMessage());
            return false;
        }
    }

    public void delete(String key) {
        try {
            stringRedisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("Redis 删除失败，key={}：{}", key, e.getMessage());
        }
    }

    public void delete(List<String> keys) {
        try {
            stringRedisTemplate.delete(keys);
        } catch (Exception e) {
            log.warn("Redis 批量删除失败：{}", e.getMessage());
        }
    }

    /**
     * 从集合中移除元素（秒杀失败回滚用）。
     */
    public void sRem(String key, String... values) {
        if (values == null || values.length == 0) {
            return;
        }
        try {
            stringRedisTemplate.opsForSet().remove(key, (Object[]) values);
        } catch (Exception e) {
            log.warn("Redis sRem 失败，key={}：{}", key, e.getMessage());
        }
    }

    public Long increment(String key, long delta) {
        try {
            return stringRedisTemplate.opsForValue().increment(key, delta);
        } catch (Exception e) {
            log.warn("Redis 自增失败，key={}：{}", key, e.getMessage());
            return null;
        }
    }

    public boolean hasKey(String key) {
        try {
            return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 执行 Lua 脚本（秒杀核心）。
     */
    public Long execute(DefaultRedisScript<Long> script, List<String> keys, String... args) {
        try {
            return stringRedisTemplate.execute(script, keys, (Object[]) args);
        } catch (Exception e) {
            log.warn("Redis 脚本执行失败，keys={}：{}", keys, e.getMessage());
            return null;
        }
    }

    /**
     * 读取 JSON 缓存。
     */
    public <T> T getJson(String key, TypeReference<T> typeReference) {
        String json = get(key);
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, typeReference);
        } catch (Exception e) {
            log.warn("Redis JSON 反序列化失败，key={}：{}", key, e.getMessage());
            return null;
        }
    }

    /**
     * 写入 JSON 缓存。
     */
    public void setJson(String key, Object value, Duration ttl) {
        try {
            set(key, objectMapper.writeValueAsString(value), ttl);
        } catch (Exception e) {
            log.warn("Redis JSON 序列化失败，key={}：{}", key, e.getMessage());
        }
    }

    /**
     * Redis 是否可用（用于秒杀降级判断）。
     */
    public boolean available() {
        return probe() == null;
    }

    /**
     * 连通性探测：返回 null 表示正常，否则返回失败原因（用于启动自检日志）。
     */
    public String probe() {
        try {
            String pong = stringRedisTemplate.execute(connection -> connection.ping(), true);
            return "PONG".equalsIgnoreCase(pong) ? null : "PING 返回 " + pong;
        } catch (Exception e) {
            Throwable root = e;
            while (root.getCause() != null && root.getCause() != root) {
                root = root.getCause();
            }
            return root.getClass().getSimpleName() + ": " + root.getMessage();
        }
    }
}
