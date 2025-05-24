package com.modive.rewardservice.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.HandlerInterceptor;

@TestConfiguration
public class TestInterceptorConfig {
    @Bean
    public HandlerInterceptor userIdInterceptor() {
        return new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                // 테스트용 사용자 ID 설정
                request.setAttribute("userId", 1L);
                return true;
            }
        };
    }
}