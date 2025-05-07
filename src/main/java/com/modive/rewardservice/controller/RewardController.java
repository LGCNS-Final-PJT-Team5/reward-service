package com.modive.rewardservice.controller;

import com.modive.rewardservice.dto.RewardEarnRequest;
import com.modive.rewardservice.dto.MonthlyRewardStatsDTO;
import com.modive.rewardservice.dto.TotalIssuedDTO;
import com.modive.rewardservice.dto.RewardHistorySimpleDTO;
import com.modive.rewardservice.dto.SearchRewardResultDTO;
import com.modive.rewardservice.entity.Reward;
import com.modive.common.Response;
import com.modive.rewardservice.service.RewardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/reward")
@RequiredArgsConstructor
public class RewardController {

    private final RewardService rewardService;

    @PostMapping("/earn")
    public ResponseEntity<Response<Reward>> earnReward(@RequestBody RewardEarnRequest request) {
        Reward reward = rewardService.earnReward(request);
        return ResponseEntity.ok(Response.success(200, "씨앗 적립에 성공하였습니다.", reward));
    }

    @GetMapping("/total")
    public ResponseEntity<Response<Map<String, Integer>>> getUserTotal(@RequestParam String userId) {
        int total = rewardService.getUserTotal(userId);
        Map<String, Integer> result = Map.of("total", total);
        return ResponseEntity.ok(Response.success(200, "사용자 잔액 조회에 성공하였습니다.", result));
    }

    @GetMapping("/history")
    public ResponseEntity<Response<Map<String, List<Reward>>>> getUserHistory(@RequestParam String userId) {
        List<Reward> history = rewardService.getUserHistory(userId);
        return ResponseEntity.ok(Response.success(200, "사용자 히스토리 조회에 성공하였습니다.", Map.of("history", history)));
    }

    @GetMapping("/total-issued")
    public ResponseEntity<Response<Map<String, TotalIssuedDTO>>> getTotalIssued() {
        TotalIssuedDTO dto = rewardService.getTotalIssued();
        return ResponseEntity.ok(Response.success(200, "씨앗 총 발급 수 조회에 성공하였습니다.", Map.of("totalIssued", dto)));
    }

    @GetMapping("/montly-issued")
    public ResponseEntity<Response<Map<String, Object>>> getMonthlyIssued() {
        Map<String, Object> result = rewardService.getMonthlyIssuedWithChangeRate();
        return ResponseEntity.ok(Response.success(200, "씨앗 월간 발급 수 조회에 성공했습니다.", Map.of("montlyIssued", result)));
    }

    @GetMapping("/daily-average-issued")
    public ResponseEntity<Response<Map<String, Object>>> getDailyAverageIssued() {
        Map<String, Object> result = rewardService.getDailyAverageIssuedWithChangeRate();
        return ResponseEntity.ok(Response.success(200, "최근 씨앗 발급 내역 조회에 성공했습니다.", Map.of("dailyAverageIssued", result)));
    }

    @GetMapping("/per-user-average-issued")
    public ResponseEntity<Response<Map<String, Object>>> getPerUserAverageIssued() {
        Map<String, Object> result = rewardService.getPerUserAverageIssuedWithChangeRate();
        return ResponseEntity.ok(Response.success(200, "최근 씨앗 발급 내역 조회에 성공했습니다.", Map.of("perUserAverageIssued", result)));
    }

    @GetMapping("/by-reason/total")
    public ResponseEntity<Response<Map<String, Object>>> getIssuedByReason() {
        List<Map<String, Object>> result = rewardService.getIssuedByReasonFormatted();
        return ResponseEntity.ok(Response.success(200, "발급 사유별 월별 통계에 성공했습니다.", Map.of("totalRewardStatics", result)));
    }

    @GetMapping("/monthly-stats")
    public ResponseEntity<Response<Map<String, List<MonthlyRewardStatsDTO>>>> getMonthlyStats(@RequestParam int year, @RequestParam int month) {
        List<MonthlyRewardStatsDTO> stats = rewardService.getMonthlyStats(year, month);
        return ResponseEntity.ok(Response.success(200, "발급 사유별 월별 통계에 성공했습니다.", Map.of("monthlyRewardStatics", stats)));
    }

    @GetMapping("/history/all")
    public ResponseEntity<Response<Map<String, List<RewardHistorySimpleDTO>>>> getAllHistory(@RequestParam int page, @RequestParam int pageSize) {
        List<RewardHistorySimpleDTO> result = rewardService.getAllHistorySimple(page, pageSize);
        return ResponseEntity.ok(Response.success(200, "최근 씨앗 발급 내역 조회에 성공했습니다.", Map.of("rewardHistory", result)));
    }

    @GetMapping("/{rewardId}")
    public ResponseEntity<Response<Map<String, Object>>> getRewardById(@PathVariable Long rewardId) {
        Reward reward = rewardService.getRewardById(rewardId);
        return ResponseEntity.ok(Response.success(200, "리워드 단건 조회에 성공하였습니다.", Map.of("reward", reward)));
    }

    @GetMapping
    public ResponseEntity<Response<Map<String, List<SearchRewardResultDTO>>>> searchReward(@RequestParam int searchKeyword) {
        List<SearchRewardResultDTO> result = rewardService.searchReward(searchKeyword);
        return ResponseEntity.ok(Response.success(200, "씨앗 사유별 월별 통계에 성공했습니다.", Map.of("searchResult", result)));
    }
}

