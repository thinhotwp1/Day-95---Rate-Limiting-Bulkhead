package com.fortna.wes.ratelimitbulkhead;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

@RestController
public class TestController {
    @Autowired
    private RateLimitService rateLimitService;

    @GetMapping("/test")
    public String test(HttpServletRequest request) {
        String ip = request.getRemoteAddr();

        rateLimitService.executeWithIPLimit(ip, () -> System.out.println("Thực thi logic quan trọng cho IP: " + ip));

        return "Success";
    }
}