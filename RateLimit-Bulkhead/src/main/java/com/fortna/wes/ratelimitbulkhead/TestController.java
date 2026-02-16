package com.fortna.wes.ratelimitbulkhead;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

@RestController
public class TestController {
    @Autowired
    private BusinessService bussinessService;

    @GetMapping("/test-rate-limit")
    public String testRateLimit(HttpServletRequest request) {
        String ip = request.getRemoteAddr();

        bussinessService.executeWithIPLimit(ip, () -> System.out.println("Thực thi logic quan trọng cho IP: " + ip));

        return "Success";
    }

    @GetMapping("/test-bulkhead")
    public String testBulkhead(HttpServletRequest request) {

        bussinessService.executeWithBulkhead(Thread.currentThread().getName(), () -> {
            try {
                Thread.sleep(5000L);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            System.out.println("Thực thi logic, thread: " + Thread.currentThread().getName());
        });

        return "Success";
    }
}