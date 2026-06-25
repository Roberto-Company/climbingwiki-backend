package com.roberto.climbingwiki.project1.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Component;

@Component
public class LoginRateLimiterService {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final ConcurrentMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public boolean tryConsume(String key) {
        return buckets.computeIfAbsent(key, k -> newBucket()).tryConsume(1);
    }

    private Bucket newBucket() {
        Bandwidth limit =
                Bandwidth.builder().capacity(MAX_ATTEMPTS).refillGreedy(MAX_ATTEMPTS, WINDOW).build();
        return Bucket.builder().addLimit(limit).build();
    }
}
