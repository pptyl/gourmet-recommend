package com.gourmet.mq;

import com.gourmet.config.SeckillProperties;
import com.gourmet.service.CouponSeckillService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 抢券订单消费者。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeckillOrderConsumer {

    private final StringRedisTemplate stringRedisTemplate;
    private final CouponSeckillService seckillService;
    private final SeckillProperties seckillProperties;

    /** Redis 不可用时的重试时间 */
    private volatile long retryAfter = 0;

    @PostConstruct
    public void initGroup() {
        try {
            stringRedisTemplate.opsForStream()
                    .createGroup(seckillProperties.getStreamKey(), seckillProperties.getConsumerGroup());
            log.info("创建订单消费组成功：{} / {}", seckillProperties.getStreamKey(), seckillProperties.getConsumerGroup());
        } catch (Exception e) {
            retryAfter = System.currentTimeMillis() + 5000;
            log.debug("订单消费组初始化跳过：{}", e.getMessage());
        }
    }

    /** 拉取并处理订单消息 */
    @Scheduled(fixedDelay = 100)
    public void consume() {
        if (System.currentTimeMillis() < retryAfter) {
            return;
        }
        int total = 0;
        for (int round = 0; round < 20; round++) {
            List<MapRecord<String, Object, Object>> records;
            try {
                records = stringRedisTemplate.opsForStream().read(
                        Consumer.from(seckillProperties.getConsumerGroup(), seckillProperties.getConsumerName()),
                        StreamReadOptions.empty()
                                .count(seckillProperties.getBatchSize())
                                .block(Duration.ofMillis(100)),
                        StreamOffset.create(seckillProperties.getStreamKey(), ReadOffset.lastConsumed()));
            } catch (Exception e) {
                retryAfter = System.currentTimeMillis() + 5000;
                return;
            }
            if (records == null || records.isEmpty()) {
                break;
            }

            List<RecordId> acked = new ArrayList<>();
            for (MapRecord<String, Object, Object> record : records) {
                Map<Object, Object> value = record.getValue();
                try {
                    String requestId = str(value.get("requestId"));
                    Long userId = Long.valueOf(str(value.get("userId")));
                    Long couponId = Long.valueOf(str(value.get("couponId")));
                    long receiveTime = Long.parseLong(str(value.getOrDefault("receiveTime", "0")));
                    seckillService.handleOrder(requestId, userId, couponId, receiveTime);
                    acked.add(record.getId());
                    total++;
                } catch (Exception e) {
                    log.error("订单消费失败，recordId={}", record.getId(), e);
                }
            }
            if (!acked.isEmpty()) {
                stringRedisTemplate.opsForStream().acknowledge(
                        seckillProperties.getStreamKey(),
                        seckillProperties.getConsumerGroup(),
                        acked.toArray(new RecordId[0]));
            }
            if (records.size() < seckillProperties.getBatchSize()) {
                break;
            }
        }
        if (total > 0) {
            log.debug("异步落库完成 {} 条订单", total);
        }
    }

    private String str(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
