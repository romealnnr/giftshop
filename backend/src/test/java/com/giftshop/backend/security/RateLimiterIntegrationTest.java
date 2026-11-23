package com.giftshop.backend.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.giftshop.backend.TestcontainersConfiguration;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class RateLimiterIntegrationTest {

    private static final Duration ONE_MINUTE = Duration.ofMinutes(1);

    @Autowired
    private RateLimiter rateLimiter;

    @Autowired
    private StringRedisTemplate redis;

    /** Each test uses its own identifier, so that the counters never collide. */
    private static String newIdentifier() {
        return UUID.randomUUID().toString();
    }

    @Test
    void allowsAttemptsUpToTheLimit() {
        String identifier = newIdentifier();

        for (int i = 0; i < 3; i++) {
            assertThatCode(() -> rateLimiter.check("test", identifier, 3, ONE_MINUTE))
                .doesNotThrowAnyException();
        }
    }

    @Test
    void blocksAttemptsAboveTheLimit_andSaysWhenToRetry() {
        String identifier = newIdentifier();
        for (int i = 0; i < 3; i++) {
            rateLimiter.check("test", identifier, 3, ONE_MINUTE);
        }

        assertThatThrownBy(() -> rateLimiter.check("test", identifier, 3, ONE_MINUTE))
            .isInstanceOfSatisfying(TooManyRequestsException.class, ex ->
                assertThat(ex.retryAfterSeconds()).isBetween(1L, 60L));
    }

    @Test
    void countsEachIdentifierSeparately() {
        String attacker = newIdentifier();
        String honestUser = newIdentifier();
        for (int i = 0; i < 3; i++) {
            rateLimiter.check("test", attacker, 3, ONE_MINUTE);
        }

        assertThatCode(() -> rateLimiter.check("test", honestUser, 3, ONE_MINUTE))
            .doesNotThrowAnyException();
    }

    @Test
    void counterExpiresAtTheEndOfTheWindow() {
        String identifier = newIdentifier();

        rateLimiter.check("test", identifier, 3, ONE_MINUTE);

        Long ttl = redis.getExpire(RateLimiter.KEY_PREFIX + "test:" + identifier, TimeUnit.SECONDS);
        assertThat(ttl).isBetween(1L, 60L);
    }
}
