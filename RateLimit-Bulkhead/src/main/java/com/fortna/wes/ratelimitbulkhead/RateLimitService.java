package com.fortna.wes.ratelimitbulkhead;

import org.springframework.stereotype.Service;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;

@Service
public class RateLimitService {

    private final RateLimiterRegistry rateLimiterRegistry;

    public RateLimitService(RateLimiterRegistry rateLimiterRegistry) {
        this.rateLimiterRegistry = rateLimiterRegistry;
    }

    public void executeWithIPLimit(String clientIp, Runnable task) {
        RateLimiter limiter = rateLimiterRegistry.rateLimiter("limit_" + clientIp, "backendApi");

        // Lắng nghe sự kiện (Chỉ cần đăng ký 1 lần hoặc dùng if để tránh đăng ký trùng)
        limiter.getEventPublisher()
                .onSuccess(event -> System.out.println("IP " + clientIp + " allowed"))
                .onFailure(event -> System.err.println("IP " + clientIp + " BLOCKED! - " + event.getEventType()));

        RateLimiter.waitForPermission(limiter);
        task.run();
    }
}
