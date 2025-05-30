package com.modive.rewardservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "user-service", url = "http://localhost:8083")
public interface UserClient {

    /**
     * 이메일로 사용자 ID 조회
     * @param email 사용자 이메일
     * @return 사용자 ID (없으면 null 또는 예외)
     */
    @GetMapping("/users/email")
    String getUserIdByEmail(@RequestParam("email") String email);

    // 🤔 아래 메서드들은 다른 서비스에서 사용 중인지 확인 후 제거 여부 결정

    /**
     * 사용자 ID로 이메일 조회
     * @param userId 사용자 ID
     * @return 사용자 이메일
     */
    @GetMapping("/users/{userId}/email")
    String getEmailByUserId(@PathVariable("userId") String userId);

    /**
     * 사용자 존재 여부 확인
     * @param userId 사용자 ID
     * @return 존재 여부
     */
    @GetMapping("/users/{userId}/exists")
    boolean existsById(@PathVariable("userId") String userId);
}