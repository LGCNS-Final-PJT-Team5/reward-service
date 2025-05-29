package com.modive.rewardservice.controller;

import com.modive.common.Response;
import com.modive.rewardservice.dto.AdminRewardDto;
import com.modive.rewardservice.service.AdminRewardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDate;

@Slf4j
@RestController
@RequestMapping("/reward")
@RequiredArgsConstructor
@Validated
public class AdminRewardController {

    private final AdminRewardService adminRewardService;

    // ===== 통계 관련 API =====

    /**
     * 1. GET /reward/stats/total - 총 발급 수 조회
     */
    @GetMapping("/stats/total")
    public ResponseEntity<Response<AdminRewardDto.TotalIssuedResponse>> getTotalStats() {
        Long totalIssued = adminRewardService.getTotalIssued();
        Double changeRate = adminRewardService.getChangeRate();

        AdminRewardDto.TotalIssuedResponse response = AdminRewardDto.TotalIssuedResponse.of(totalIssued, changeRate);

        return ResponseEntity.ok(Response.success(200, "씨앗 총 발급 수 조회에 성공했습니다.", response));
    }

    /**
     * 2. GET /reward/stats/monthly - 월간 발급 수 조회
     */
    @GetMapping("/stats/monthly")
    public ResponseEntity<Response<AdminRewardDto.MonthlyIssuedResponse>> getMonthlyStats() {
        Long monthlyIssued = adminRewardService.getCurrentMonthIssued();
        Double changeRate = adminRewardService.getMonthlyChangeRate();

        AdminRewardDto.MonthlyIssuedResponse response = AdminRewardDto.MonthlyIssuedResponse.of(monthlyIssued, changeRate);

        return ResponseEntity.ok(Response.success(200, "씨앗 월간 발급 수 조회에 성공했습니다.", response));
    }

    /**
     * 3. GET /reward/stats/daily - 일 평균 발급 수 조회
     */
    @GetMapping("/stats/daily")
    public ResponseEntity<Response<AdminRewardDto.DailyAverageIssuedResponse>> getDailyStats() {
        Double dailyAverageIssued = adminRewardService.getCurrentDailyAverageIssued();
        Double changeRate = adminRewardService.getDailyAverageChangeRate();

        AdminRewardDto.DailyAverageIssuedResponse response = AdminRewardDto.DailyAverageIssuedResponse.of(dailyAverageIssued, changeRate);

        return ResponseEntity.ok(Response.success(200, "씨앗 일 평균 발급 수 조회에 성공했습니다.", response));
    }

    /**
     * 4. GET /reward/stats/per-user - 사용자당 평균 발급 수 조회
     */
    @GetMapping("/stats/per-user")
    public ResponseEntity<Response<AdminRewardDto.PerUserAverageIssuedResponse>> getPerUserStats() {
        Double perUserAverageIssued = adminRewardService.getCurrentPerUserAverageIssued();
        Double changeRate = adminRewardService.getPerUserAverageChangeRate();

        AdminRewardDto.PerUserAverageIssuedResponse response = AdminRewardDto.PerUserAverageIssuedResponse.of(perUserAverageIssued, changeRate);

        return ResponseEntity.ok(Response.success(200, "씨앗 사용자당 평균 발급 수 조회에 성공했습니다.", response));
    }

    /**
     * 5. GET /reward/by-reason/total - 발급 사유별 총 통계 조회
     */
    @GetMapping("/by-reason/total")
    public ResponseEntity<Response<AdminRewardDto.TotalReasonStatsResponse>> getTotalReasonStats() {
        AdminRewardDto.TotalReasonStatsResponse response = adminRewardService.getTotalRewardStats();

        return ResponseEntity.ok(Response.success(200, "리워드 발급 사유별 통계 조회에 성공했습니다.", response));
    }

    /**
     * 6. GET /reward/by-reason/monthly - 발급 사유별 월별 통계 조회
     */
    @GetMapping("/by-reason/monthly")
    public ResponseEntity<Response<AdminRewardDto.MonthlyReasonStatsResponse>> getMonthlyReasonStats(
            @RequestParam(value = "month", required = false) String month) {
        AdminRewardDto.MonthlyReasonStatsResponse response = adminRewardService.getMonthlyRewardStatsByReason(month);

        return ResponseEntity.ok(Response.success(200, "리워드 발급 사유별 월별 통계 조회에 성공했습니다.", response));
    }

    /**
     * 7. GET /reward/monthly-stats - 월별 씨앗 지급 통계 조회
     */
    @GetMapping("/monthly-stats")
    public ResponseEntity<Response<AdminRewardDto.MonthlyStatsResponse>> getMonthlyHistoryStats() {
        AdminRewardDto.MonthlyStatsResponse response = adminRewardService.getMonthlyRewardTrends();

        return ResponseEntity.ok(Response.success(200, "월별 씨앗 지급 통계 조회에 성공하였습니다.", response));
    }

    /**
     * 8. GET /reward/history/all - 최근 씨앗 발급 내역 조회
     */
    @GetMapping("/history/all")
    public ResponseEntity<Response<AdminRewardDto.AllRewardHistoryResponse>> getAllRewardHistory(
            @PageableDefault(size = 10) Pageable pageable) {

        Page<AdminRewardDto.AllRewardHistoryResponse.RewardHistoryItem> page =
                adminRewardService.getAllRewardHistory(pageable);

        AdminRewardDto.AllRewardHistoryResponse response = AdminRewardDto.AllRewardHistoryResponse.of(page);

        return ResponseEntity.ok(Response.success(200, "전체 씨앗 발급 내역 조회에 성공했습니다.", response));
    }

    /**
     * 9. GET /reward/filter - 씨앗 필터링 조회
     */
    @GetMapping("/filter")
    public ResponseEntity<Response<AdminRewardDto.RewardFilterResponse>> filterRewardHistory(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @PageableDefault(size = 10) Pageable pageable) {

        AdminRewardDto.RewardFilterResponse response = adminRewardService.filterRewards(
                email, description, startDate, endDate, pageable);

        return ResponseEntity.ok(Response.success(200, "씨앗 발급 내역 검색에 성공했습니다.", response));
    }

    /**
     * 10. POST /reward/by-drive - 운전별 씨앗 적립 내역 조회
     */
    @PostMapping("/by-drive")
    public ResponseEntity<Response<AdminRewardDto.RewardsByDriveResponse>> getRewardHistoryByDrive(
            @Valid @RequestBody AdminRewardDto.RewardsByDriveRequest request) {

        AdminRewardDto.RewardsByDriveResponse response = adminRewardService.getRewardsByDrive(request);

        return ResponseEntity.ok(Response.success(200, "운전별 씨앗 적립 내역 조회에 성공하였습니다.", response));
    }
}