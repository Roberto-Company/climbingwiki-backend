package com.roberto.climbingwiki.project1.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LoginRateLimiterServiceTest {

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
}
