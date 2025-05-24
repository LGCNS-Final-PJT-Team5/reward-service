package com.modive.rewardservice.controller;

import com.modive.common.Response;
import com.modive.rewardservice.service.AdminRewardService;
import com.modive.rewardservice.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/reward")
@RequiredArgsConstructor
public class AdminRewardController {

    private final AdminRewardService adminRewardService;

    @GetMapping("/total-issued")
    public ResponseEntity<Response<AdminRewardDto.TotalIssuedResponse>> getTotalIssued() {
        long total = adminRewardService.getTotalIssued();
        double changeRate = adminRewardService.getChangeRate();
        return ResponseEntity.ok(Response.success(200, "씨앗 총 발급 수 조회에 성공했습니다.",
                AdminRewardDto.TotalIssuedResponse.of(total, changeRate)));
    }

    @GetMapping("/monthly-issued")
    public ResponseEntity<Response<AdminRewardDto.MonthlyIssuedResponse>> getMonthlyIssued() {
        long value = adminRewardService.getCurrentMonthIssued();
        double changeRate = adminRewardService.getMonthlyChangeRate();
        return ResponseEntity.ok(Response.success(200, "씨앗 월간 발급 수 조회에 성공했습니다.",
                AdminRewardDto.MonthlyIssuedResponse.of(value, changeRate)));
    }

    @GetMapping("/daily-average-issued")
    public ResponseEntity<Response<?>> getDailyAverageIssued() {
        double value = adminRewardService.getCurrentDailyAverageIssued();
        double changeRate = adminRewardService.getDailyAverageChangeRate();

        var response = Map.of("dailyAverageIssued", Map.of(
                "value", value,
                "changeRate", changeRate
        ));

        return ResponseEntity.ok(Response.success(200, "씨앗 일 평균 발급 수 조회에 성공했습니다.", response));
    }

    @GetMapping("/per-user-average-issued")
    public ResponseEntity<Response<AdminRewardDto.PerUserAverageIssuedResponse>> getPerUserAverageIssued() {
        double value = adminRewardService.getCurrentPerUserAverageIssued();
        double changeRate = adminRewardService.getPerUserAverageChangeRate();
        return ResponseEntity.ok(Response.success(200, "씨앗 사용자당 평균 발급 수 조회에 성공했습니다.",
                AdminRewardDto.PerUserAverageIssuedResponse.of(value, changeRate)));
    }

    @GetMapping("/by-reason/total")
    public ResponseEntity<Response<AdminRewardDto.TotalReasonStatsResponse>> getTotalIssuedByReason() {
        List<AdminRewardDto.TotalReasonStatsResponse.ReasonStat> stats = adminRewardService.getTotalIssuedByReason();
        return ResponseEntity.ok(Response.success(200, "리워드 발급 사유별 통계 조회에 성공했습니다.",
                AdminRewardDto.TotalReasonStatsResponse.of(stats)));
    }

    @GetMapping("/monthly-stats")
    public ResponseEntity<Response<AdminRewardDto.MonthlyStatsResponse>> getMonthlyStats() {
        AdminRewardDto.MonthlyStatsResponse response = adminRewardService.getMonthlyRewardStats();
        return ResponseEntity.ok(Response.success(200, "월별 씨앗 지급 통계 조회에 성공하였습니다.", response));
    }

    @GetMapping("/history/all")
    public ResponseEntity<Response<AdminRewardDto.AllRewardHistoryResponse>> getAllRewardHistory(Pageable pageable) {
        Page<AdminRewardDto.AllRewardHistoryResponse.RewardHistoryItem> page = adminRewardService.getAllRewardHistory(pageable);
        AdminRewardDto.AllRewardHistoryResponse response = AdminRewardDto.AllRewardHistoryResponse.of(page);
        return ResponseEntity.ok(Response.success(200, "전체 씨앗 발급 내역 조회에 성공했습니다.", response));
    }

    @PostMapping("/by-drive")
    public ResponseEntity<Response<AdminRewardDto.RewardsByDriveResponse>> getRewardsByDrive(
            @RequestBody AdminRewardDto.RewardsByDriveRequest request) {
        AdminRewardDto.RewardsByDriveResponse response = adminRewardService.getRewardsByDrive(request);
        return ResponseEntity.ok(Response.success(200, "운전별 씨앗 적립 내역 조회에 성공하였습니다.", response));
    }

    @GetMapping("/filter")
    public ResponseEntity<Response<AdminRewardDto.RewardFilterResponse>> filterRewards(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            Pageable pageable) {
        AdminRewardDto.RewardFilterResponse response = adminRewardService.filterRewards(email, description, startDate, endDate, pageable);
        return ResponseEntity.ok(Response.success(200, "씨앗 발급 내역 검색에 성공했습니다.", response));
    }
}