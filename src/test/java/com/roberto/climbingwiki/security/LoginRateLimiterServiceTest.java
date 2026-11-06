package com.roberto.climbingwiki.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class LoginRateLimiterServiceTest {

    /** Clock whose time the test moves forward by hand. */
    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-11-05T10:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }
    }

    @Test
    void tryConsume_underLimit_allowsEachAttempt() {
        LoginRateLimiterService limiter = new LoginRateLimiterService();

        for (int i = 0; i < 5; i++) {
            assertThat(limiter.tryConsume("1.2.3.4")).isTrue();
        }
    }

    @Test
    void tryConsume_overLimit_rejectsFurtherAttempts() {
        LoginRateLimiterService limiter = new LoginRateLimiterService();

        for (int i = 0; i < 5; i++) {
            limiter.tryConsume("1.2.3.4");
        }

        assertThat(limiter.tryConsume("1.2.3.4")).isFalse();
    }

    @Test
    void tryConsume_differentKeys_areTrackedIndependently() {
        LoginRateLimiterService limiter = new LoginRateLimiterService();
        for (int i = 0; i < 5; i++) {
            limiter.tryConsume("1.2.3.4");
        }

        assertThat(limiter.tryConsume("5.6.7.8")).isTrue();
    }

    @Test
    void tryConsume_idleKeys_areEvictedAfterIdleTimeout() {
        MutableClock clock = new MutableClock();
        LoginRateLimiterService limiter = new LoginRateLimiterService(clock);
        limiter.tryConsume("1.1.1.1");
        limiter.tryConsume("2.2.2.2");

        clock.advance(LoginRateLimiterService.IDLE_TIMEOUT.plusSeconds(1));
        limiter.tryConsume("3.3.3.3");

        assertThat(limiter.trackedKeyCount()).isEqualTo(1);
    }

    @Test
    void tryConsume_recentlyUsedKeys_areKept() {
        MutableClock clock = new MutableClock();
        LoginRateLimiterService limiter = new LoginRateLimiterService(clock);
        limiter.tryConsume("1.1.1.1");

        clock.advance(LoginRateLimiterService.IDLE_TIMEOUT.minusSeconds(10));
        limiter.tryConsume("1.1.1.1");
        clock.advance(Duration.ofSeconds(20));
        limiter.tryConsume("2.2.2.2");

        assertThat(limiter.trackedKeyCount()).isEqualTo(2);
    }

    @Test
    void tryConsume_blockedKeyStaysBlockedWhileStillActive() {
        MutableClock clock = new MutableClock();
        LoginRateLimiterService limiter = new LoginRateLimiterService(clock);
        for (int i = 0; i < 5; i++) {
            limiter.tryConsume("1.2.3.4");
        }

        clock.advance(Duration.ofSeconds(5));

        assertThat(limiter.tryConsume("1.2.3.4")).isFalse();
    }
}
