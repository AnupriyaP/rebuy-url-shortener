package com.rebuy.urlshortener.service;

import com.rebuy.urlshortener.exception.RateLimitExceededException;
import io.github.bucket4j.Bucket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-IP rate limiter using Bucket4j token bucket algorithm.
 *
 * Each IP gets its own bucket with:
 *   - capacity: max requests allowed at once
 *   - refill:   tokens added per minute
 *
 * Why per-IP:
 *   - Prevents one client from exhausting hash space
 *   - Protects DB from scripted abuse
 *   - Real users never hit the limit (10 req/min is generous)
 *
 * KNOWN LIMITATION: buckets map grows unbounded — one entry per
 * unique IP ever seen, never evicted. Acceptable for MVP demo
 * traffic; production would need a TTL-based cache (e.g. Caffeine)
 * or move this to Redis so limits work across multiple instances.
 */
@Service
@Slf4j
public class RateLimiterService {

    @Value("${app.rate-limit.capacity:10}")
    private int capacity;

    @Value("${app.rate-limit.refill-per-minute:10}")
    private int refillPerMinute;

    // One bucket per IP address
    private final ConcurrentHashMap<String, Bucket> buckets
            = new ConcurrentHashMap<>();

    /**
     * Check if the IP is allowed to make a request.
     * Throws RateLimitExceededException if limit exceeded.
     */
    public void checkLimit(String ip) {
        Bucket bucket = buckets.computeIfAbsent(ip, this::newBucket);
        if (!bucket.tryConsume(1)) {
            log.warn("Rate limit exceeded for IP: {} (bucket map size: {})",
                    ip, buckets.size());
            throw new RateLimitExceededException(ip);
        }
        log.trace("Rate limit check passed for IP: {}", ip);
    }

    private Bucket newBucket(String ip) {
        log.debug("Creating new rate limit bucket for IP: {} (capacity={}, refill={}/min)",
                ip, capacity, refillPerMinute);
        return Bucket.builder()
                .addLimit(limit -> limit
                        .capacity(capacity)
                        .refillGreedy(refillPerMinute, Duration.ofMinutes(1))
                )
                .build();
    }
}