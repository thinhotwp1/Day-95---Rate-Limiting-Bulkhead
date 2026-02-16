package com.fortna.wes.ratelimitbulkhead;

import org.springframework.stereotype.Service;

import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;

@Service
public class BusinessService {

    private final RateLimiterRegistry rateLimiterRegistry;
    private final BulkheadRegistry bulkheadRegistry; // Thêm Registry này

    public BusinessService(RateLimiterRegistry rateLimiterRegistry, BulkheadRegistry bulkheadRegistry) {
        this.rateLimiterRegistry = rateLimiterRegistry;
        this.bulkheadRegistry = bulkheadRegistry;
    }

    // Hàm chỉ dùng Ratelimit
    public void executeWithIPLimit(String clientIp, Runnable task) {
        RateLimiter limiter = rateLimiterRegistry.rateLimiter("limit_" + clientIp, "backendApi");

        // Lắng nghe sự kiện (Chỉ cần đăng ký 1 lần hoặc dùng if để tránh đăng ký trùng)
        limiter.getEventPublisher()
                .onSuccess(event -> System.out.println("IP " + clientIp + " allowed"))
                .onFailure(event -> System.err.println("IP " + clientIp + " BLOCKED! - " + event.getEventType()));

        RateLimiter.waitForPermission(limiter);
        task.run();
    }

    // Hàm chỉ dùng Bulkhead
    public void executeWithBulkhead(String threadName, Runnable task) {
        // Tạo/Lấy Bulkhead instance dựa trên "khuôn" backendApi
        Bulkhead bulkhead = bulkheadRegistry.bulkhead("bulk_execute_with_bulkhead" , "backendApi");

        // Chạy task trong vách ngăn (Bulkhead)
        bulkhead.executeRunnable(task);
    }

    // CÁCH TỐI ƯU: Kết hợp cả Rate Limiter và Bulkhead (Bảo vệ 2 lớp)
    public void executeWithFullProtection(String clientIp, Runnable task) {
        RateLimiter limiter = rateLimiterRegistry.rateLimiter("limit_" + clientIp, "backendApi");
        Bulkhead bulkhead = bulkheadRegistry.bulkhead("bulk_" + clientIp, "backendApi");

        // Bọc task: Đầu tiên check Rate Limit -> Sau đó check Bulkhead
        Runnable combinedTask = RateLimiter.decorateRunnable(limiter,
                Bulkhead.decorateRunnable(bulkhead, task));

        combinedTask.run();
    }
}
