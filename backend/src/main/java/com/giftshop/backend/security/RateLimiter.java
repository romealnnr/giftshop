package com.giftshop.backend.security;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Limits how many times an action can be attempted, with a fixed window counter
 * stored in Redis (shared by all the instances of the application).
 *
 * <p>Fails open: if Redis is unavailable, the request is allowed and a warning is
 * logged. Rate limiting is an extra protection (passwords are hashed, gift codes are
 * long), so an outage of Redis must not block every user.
 */
@Component
public class RateLimiter {

    private static final Logger log = LoggerFactory.getLogger(RateLimiter.class);

    static final String KEY_PREFIX = "giftshop:rate-limit:";

    private final StringRedisTemplate redis;

    public RateLimiter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /**
     * Counts one more attempt of the action for the identifier.
     *
     * @param action     what is attempted, for example "login"
     * @param identifier who attempts it, for example an IP address or a user id
     * @param limit      the number of attempts allowed in the window
     * @param window     the duration after which the counter starts again from zero
     * @throws TooManyRequestsException if the limit is exceeded
     */
    public void check(String action, String identifier, int limit, Duration window) {
        String key = KEY_PREFIX + action + ":" + identifier;

        long attempts;
        long retryAfterSeconds;
        try {
            // Creates the counter with its expiry only if it does not exist yet,
            // so that a new attempt never extends the window.
            redis.opsForValue().setIfAbsent(key, "0", window);
            Long count = redis.opsForValue().increment(key);
            attempts = (count == null) ? 0 : count;

            Long ttl = redis.getExpire(key, TimeUnit.SECONDS);
            retryAfterSeconds = (ttl == null || ttl < 1) ? window.toSeconds() : ttl;
        } catch (DataAccessException e) {
            log.warn("Rate limiter unavailable: request allowed", e);
            return;
        }

        if (attempts > limit) {
            throw new TooManyRequestsException(retryAfterSeconds);
        }
    }
}
