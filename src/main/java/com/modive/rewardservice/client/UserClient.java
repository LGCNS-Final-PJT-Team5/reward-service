package com.modive.rewardservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(name = "user-service", url = "http://localhost:8083") // 사용자 서비스 주소
public interface UserClient {

    @GetMapping("/users/email")
    Long getUserIdByEmail(@RequestParam("email") String email);

    @GetMapping("/users/{userId}/email")
    String getEmailByUserId(@PathVariable("userId") Long userId);
}

