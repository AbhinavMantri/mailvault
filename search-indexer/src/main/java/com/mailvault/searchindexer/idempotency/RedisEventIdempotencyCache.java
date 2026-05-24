package com.mailvault.searchindexer.idempotency;

import com.mailvault.searchindexer.config.IdempotencyProperties;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Component
public class RedisEventIdempotencyCache implements EventIdempotencyCache {

    private final StringRedisTemplate redisTemplate;
    private final IdempotencyProperties properties;

    public RedisEventIdempotencyCache(StringRedisTemplate redisTemplate, IdempotencyProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    @Override
    public boolean wasRecentlyProcessed(String consumerName, String eventType, UUID eventId) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key(consumerName, eventType, eventId)));
        } catch (RedisConnectionFailureException exception) {
            return false;
        }
    }

    @Override
    public void rememberProcessed(String consumerName, String eventType, UUID eventId) {
        try {
            redisTemplate.opsForValue().set(key(consumerName, eventType, eventId), "1", ttl());
        } catch (RedisConnectionFailureException exception) {
            // Redis is an optimization. Durable idempotency comes from the target store upsert/state check.
        }
    }

    private String key(String consumerName, String eventType, UUID eventId) {
        return "%s:%s:%s:%s".formatted(properties.redisKeyPrefix(), consumerName, eventType, eventId);
    }

    private Duration ttl() {
        return Duration.ofDays(properties.ttlDays());
    }
}
