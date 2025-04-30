package com.modive.rewardservice.controller;

import com.modive.rewardservice.dto.RewardSummaryDTO;
import com.modive.rewardservice.entity.Reward;
import com.modive.common.Response;
import com.modive.rewardservice.service.RewardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reward")
@RequiredArgsConstructor
public class RewardController {

    private final RewardService rewardService;

    @GetMapping("/{userId}/earn")
    public ResponseEntity<Response<Reward>> earnReward(
            @PathVariable("userId") String userId,
            @RequestParam("amount") Integer amount,
            @RequestParam("reason") String reason,
            @RequestParam("description") String description) {

        Reward reward = rewardService.earnReward(userId, amount, reason, description);
        return ResponseEntity.ok(Response.success(reward));
    }

    @GetMapping("/{userId}/use")
    public ResponseEntity<Response<Reward>> useReward(
            @PathVariable("userId") String userId,
            @RequestParam("amount") Integer amount,
            @RequestParam("reason") String reason,
            @RequestParam("description") String description) {

        Reward reward = rewardService.useReward(userId, amount, reason, description);
        return ResponseEntity.ok(Response.success(reward));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<Response<RewardSummaryDTO>> getRewardSummary(
            @PathVariable("userId") String userId) {

        RewardSummaryDTO summary = rewardService.getRewardSummary(userId);
        return ResponseEntity.ok(Response.success(summary));
    }

    @GetMapping("/{userId}/total")
    public ResponseEntity<Response<Integer>> getTotalReward(
            @PathVariable("userId") String userId) {

        RewardSummaryDTO summary = rewardService.getRewardSummary(userId);
        return ResponseEntity.ok(Response.success(summary.getAvailableSeeds()));
    }
}