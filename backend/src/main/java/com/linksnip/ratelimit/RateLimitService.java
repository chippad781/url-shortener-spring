package com.linksnip.ratelimit;

import com.linksnip.common.RateLimitExceededException;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Fixed-window rate limiting backed by Redis INCR + EXPIRE.
 *
 * The first request in a window sets the counter and its TTL; subsequent
 * requests increment. When the count exceeds the limit we reject with 429.
 *
 * Fail-open: if Redis is unavailable we allow the request rather than taking
 * the whole API down — availability is preferred over strict enforcement for
 * this class of limit (matches the original "Redis down => fall back" design).
 */
@Service
public class RateLimitService {

    private final StringRedisTemplate redis;

    public RateLimitService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void check(String bucketKey, int limit, Duration window, String message) {
        long current;
        try {
            Long value = redis.opsForValue().increment(bucketKey);
            current = value == null ? 1L : value;
            if (current == 1L) {
                redis.expire(bucketKey, window);
            }
        } catch (DataAccessException ex) {
            return; // fail open
        }
        if (current > limit) {
            throw new RateLimitExceededException(message);
        }
    }
}
