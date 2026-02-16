package com.fortna.wes.ratelimitbulkhead;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import io.github.resilience4j.ratelimiter.RequestNotPermitted;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RequestNotPermitted.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public String handleRateLimit() {
        return "Bạn thao tác quá nhanh! Vui lòng đợi 10 giây.";
    }
}