package com.roberto.climbingwiki.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LoginRateLimiterService {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    /**
     * Keys unused for this long are dropped. Must exceed {@link #WINDOW}: by then the bucket has
     * fully refilled, so dropping it is the same as keeping it.
     */
    static final Duration IDLE_TIMEOUT = Duration.ofMinutes(10);

    private final ConcurrentMap<String, TrackedBucket> buckets = new ConcurrentHashMap<>();
    private final Clock clock;
    private volatile Instant lastSweep;

    @Autowired
    public LoginRateLimiterService() {
        this(Clock.systemUTC());
    }

    LoginRateLimiterService(Clock clock) {
        this.clock = clock;
        this.lastSweep = clock.instant();
    }

    public boolean tryConsume(String key) {
        Instant now = clock.instant();
        sweepIfDue(now);
        TrackedBucket tracked = buckets.computeIfAbsent(key, k -> new TrackedBucket(newBucket()));
        tracked.lastSeen = now;
        return tracked.bucket.tryConsume(1);
    }

    int trackedKeyCount() {
        return buckets.size();
    }

    private void sweepIfDue(Instant now) {
        if (Duration.between(lastSweep, now).compareTo(IDLE_TIMEOUT) < 0) {
            return;
        }
        lastSweep = now;
        Instant cutoff = now.minus(IDLE_TIMEOUT);
        buckets.values().removeIf(tracked -> tracked.lastSeen.isBefore(cutoff));
    }

    private Bucket newBucket() {
        Bandwidth limit =
                Bandwidth.builder().capacity(MAX_ATTEMPTS).refillGreedy(MAX_ATTEMPTS, WINDOW).build();
        return Bucket.builder().addLimit(limit).build();
    }

    private static final class TrackedBucket {
        private final Bucket bucket;
        private volatile Instant lastSeen = Instant.MIN;

        private TrackedBucket(Bucket bucket) {
            this.bucket = bucket;
        }
    }
}
