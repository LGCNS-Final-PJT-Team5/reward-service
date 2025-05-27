package com.modive.rewardservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(name = "user-service", url = "http://localhost:8083")
public interface UserClient {

    @GetMapping("/users/email")
    Long getUserIdByEmail(@RequestParam("email") String email);

    @GetMapping("/users/{userId}/email")
    String getEmailByUserId(@PathVariable("userId") Long userId);

    // 🔧 신규: 배치 조회 메서드 추가
    @PostMapping("/users/emails/batch")
    Map<Long, String> getEmailsByUserIds(@RequestBody List<Long> userIds);

    // 🔧 신규: 사용자 존재 여부 확인
    @GetMapping("/users/{userId}/exists")
    boolean existsById(@PathVariable("userId") Long userId);

    // 🔧 신규: 이메일로 사용자 존재 여부 확인
    @GetMapping("/users/exists")
    boolean existsByEmail(@RequestParam("email") String email);
}