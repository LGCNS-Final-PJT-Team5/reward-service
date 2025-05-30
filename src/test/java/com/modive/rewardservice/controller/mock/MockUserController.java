package com.modive.rewardservice.controller.mock;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

@Profile("test")  //  "test" 프로파일일 때만 이 컨트롤러가 등록됨
@RestController
@RequestMapping("/users")
public class MockUserController {

    @GetMapping("/email")
    public Long getUserIdByEmail(@RequestParam String email) {
        if (email.equals("user1@example.com")) return 1L;
        else if (email.equals("user2@example.com")) return 2L;
        return 999L;
    }

    @GetMapping("/{userId}/email")
    public String getEmailByUserId(@PathVariable String userId) {
        if (userId == 1L) return "user1@example.com";
        else if (userId == 2L) return "user2@example.com";
        return "unknown@example.com";
    }
}