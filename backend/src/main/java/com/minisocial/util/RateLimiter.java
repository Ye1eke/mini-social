package com.minisocial.util;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiter component that tracks requests per IP address.
 * Configured to allow 10 requests per minute per IP.
 */
@Component
public class RateLimiter {

    private static final int MAX_REQUESTS_PER_MINUTE = 10;
    private static final long TIME_WINDOW_SECONDS = 60;

    private final ConcurrentHashMap<String, List<Instant>> requestTracker = new ConcurrentHashMap<>();

    /**
     * Checks if a request from the given IP address should be allowed.
     * Removes expired entries older than 1 minute.
     *
     * @param ipAddress the IP address of the requester
     * @return true if the request is allowed, false if rate limit is exceeded
     */
    public boolean allowRequest(String ipAddress) {
        Instant now = Instant.now();
        Instant cutoffTime = now.minusSeconds(TIME_WINDOW_SECONDS);

        // Get or create the request list for this IP
        List<Instant> requests = requestTracker.computeIfAbsent(ipAddress, k -> new ArrayList<>());

        // Synchronize on the list to ensure thread safety
        synchronized (requests) {
            // Remove expired entries older than 1 minute
            requests.removeIf(timestamp -> timestamp.isBefore(cutoffTime));

            // Check if the request count is within the limit
            if (requests.size() >= MAX_REQUESTS_PER_MINUTE) {
                return false;
            }

            // Add the current request timestamp
            requests.add(now);
            return true;
        }
    }

    /**
     * Clears all tracking data. Useful for testing.
     */
    public void clear() {
        requestTracker.clear();
    }
}
