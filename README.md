
# Day 95: Resilience Patterns - Rate Limiting & Bulkhead

## 📌 Tổng quan

Trong một hệ thống phân tán, việc bảo vệ tài nguyên hệ thống khỏi các đợt Traffic Spike (tăng trưởng lưu lượng đột ngột) hoặc tấn công DDoS là tối quan trọng. **Day 95** tập trung vào việc triển khai hai lá chắn bảo vệ chính bằng thư viện **Resilience4j**:

1. **Rate Limiting**: Giới hạn số lượng yêu cầu trong một khoảng thời gian (Traffic Control).
2. **Bulkhead**: Giới hạn số lượng luồng (thread) xử lý đồng thời để tránh cạn kiệt tài nguyên (Resource Isolation).

---

## 🛠 Kiến trúc lá chắn (Guard Architecture)

Dự án này triển khai cơ chế **Dynamic Protection theo Client IP**, đảm bảo mỗi người dùng có một hạn mức riêng biệt, tránh tình trạng một người dùng spam làm ảnh hưởng đến toàn bộ hệ thống.

### 1. Thuật toán sử dụng

* **Rate Limiter**: Sử dụng thuật toán **Token Bucket**.
* **Bulkhead**: Sử dụng **Semaphore Bulkhead** (giới hạn số lượng truy cập đồng thời).

---

## ⚙️ Cấu hình Resilience4j (`application.yml`)

Sử dụng mục `configs` để định nghĩa các "khuôn mẫu" (blueprints), cho phép ứng dụng tạo ra hàng nghìn bộ lọc cho từng IP một cách động.

```yaml
resilience4j:
  ratelimiter:
    configs:
      backendApi:
        limitForPeriod: 1            # Số lượng request tối đa
        limitRefreshPeriod: 10s       # Chu kỳ reset hạn mức
        timeoutDuration: 0ns          # Fail-fast: báo lỗi ngay nếu hết lượt
  
  bulkhead:
    configs:
      backendApi:
        maxConcurrentCalls: 2         # Tối đa 2 request xử lý cùng lúc trên mỗi IP
        maxWaitDuration: 0ns          # Fail-fast: không chờ đợi nếu vách ngăn đầy

```

---

## 🚀 Triển khai Code

### RateLimitService

Service này chịu trách nhiệm khởi tạo và quản lý các bộ lọc theo địa chỉ IP của Client.

```java
@Service
public class RateLimitService {
    private final RateLimiterRegistry rateLimiterRegistry;
    private final BulkheadRegistry bulkheadRegistry;

    public void executeWithFullProtection(String clientIp, Runnable task) {
        // Khởi tạo instance động từ cấu hình mẫu 'backendApi'
        RateLimiter limiter = rateLimiterRegistry.rateLimiter("limit_" + clientIp, "backendApi");
        Bulkhead bulkhead = bulkheadRegistry.bulkhead("bulk_" + clientIp, "backendApi");

        // Bọc task qua 2 lớp bảo vệ
        Runnable combinedTask = RateLimiter.decorateRunnable(limiter, 
                                 Bulkhead.decorateRunnable(bulkhead, task));
        
        combinedTask.run();
    }
}

```

---

## ⚠️ Lưu ý quan trọng cho Senior

### 1. Phân biệt Rate Limiter & Bulkhead

* **Rate Limiter** chặn khách khi họ đến **quá nhanh** (ví dụ: 100 req/s).
* **Bulkhead** chặn khách khi họ đứng lại **quá lâu** (ví dụ: request chạy mất 30s làm nghẽn thread).

### 2. Vấn đề bộ nhớ (Memory Management)

Việc tạo `RateLimiter` theo IP động có thể gây ra lỗi **Memory Leak** nếu có hàng triệu IP truy cập.

* **Giải pháp**: Cần có cơ chế dọn dẹp (Eviction) các instance cũ trong Registry hoặc sử dụng Distributed Rate Limiter với Redis.

### 3. Fail-Fast vs Wait

Trong cấu hình này, `timeoutDuration` và `maxWaitDuration` được set là **0ns**. Điều này thực hiện chiến lược **Fail-Fast**: trả về lỗi ngay lập tức để giải phóng tài nguyên CPU/RAM cho các tác vụ khác thay vì bắt Thread phải chờ đợi trong vô vọng.

---

## 🧪 Cách kiểm tra (Testing)

1. **Kiểm tra Rate Limit**: Gọi API liên tục 2 lần trong vòng dưới 10 giây. Lần thứ 2 sẽ nhận mã lỗi **429 Too Many Requests**.
2. **Kiểm tra Bulkhead**: Tạo một task có `Thread.sleep(5000)`. Dùng 3 tab trình duyệt gọi cùng lúc. Tab thứ 3 sẽ bị chặn ngay lập tức do vượt quá `maxConcurrentCalls: 2`.

---

## 🛡️ Exception Handling

Hệ thống sử dụng `@RestControllerAdvice` để chuyển đổi các Exception của Resilience4j thành HTTP Status phù hợp:

* `RequestNotPermitted` -> **429 Too Many Requests**
* `BulkheadFullException` -> **429 Too Many Requests** (kèm message hệ thống bận).

---

*Kết quả của Day 95 giúp hệ thống đạt được sự ổn định (Stability) và khả năng chống chịu (Resilience) trước các điều kiện khắc nghiệt của môi trường Internet.*
