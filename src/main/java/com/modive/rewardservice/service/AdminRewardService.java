package com.modive.rewardservice.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.modive.rewardservice.client.UserClient;
import com.modive.rewardservice.domain.Reward;
import com.modive.rewardservice.domain.RewardReason;
import com.modive.rewardservice.dto.AdminRewardDto;
import com.modive.rewardservice.repository.RewardRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@EnableFeignClients(basePackages = "com.modive.rewardservice.client")
public class AdminRewardService {

    private final RewardRepository rewardRepository;
    private final UserClient userClient;

    @Transactional(readOnly = true)
    public long getTotalIssued() {
        return rewardRepository.getTotalIssued();
    }

    @Transactional(readOnly = true)
    public double getChangeRate() {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        // 오늘 발급된 리워드 수
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.atTime(23, 59, 59);
        long todayCount = rewardRepository.countIssuedBetween(todayStart, todayEnd);

        // 어제 발급된 리워드 수
        LocalDateTime yesterdayStart = yesterday.atStartOfDay();
        LocalDateTime yesterdayEnd = yesterday.atTime(23, 59, 59);
        long yesterdayCount = rewardRepository.countIssuedBetween(yesterdayStart, yesterdayEnd);

        if (yesterdayCount == 0) {
            return todayCount == 0 ? 0.0 : 100.0;
        }

        return Math.round(((double) (todayCount - yesterdayCount) / yesterdayCount) * 1000) / 10.0;
    }

    @Transactional(readOnly = true)
    public long getCurrentMonthIssued() {
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime start = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime end = currentMonth.atEndOfMonth().atTime(23, 59, 59);
        return rewardRepository.countIssuedBetween(start, end);
    }

    @Transactional(readOnly = true)
    public double getMonthlyChangeRate() {
        YearMonth currentMonth = YearMonth.now();
        YearMonth previousMonth = currentMonth.minusMonths(1);

        LocalDateTime currentStart = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime currentEnd = currentMonth.atEndOfMonth().atTime(23, 59, 59);
        long currentCount = rewardRepository.countIssuedBetween(currentStart, currentEnd);

        LocalDateTime prevStart = previousMonth.atDay(1).atStartOfDay();
        LocalDateTime prevEnd = previousMonth.atEndOfMonth().atTime(23, 59, 59);
        long prevCount = rewardRepository.countIssuedBetween(prevStart, prevEnd);

        if (prevCount == 0) {
            return currentCount == 0 ? 0.0 : 100.0;
        }

        return Math.round(((double) (currentCount - prevCount) / prevCount) * 1000) / 10.0;
    }

    @Transactional(readOnly = true)
    public double getDailyAverageChangeRate() {
        LocalDate today = LocalDate.now();

        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.atTime(LocalTime.MAX);
        LocalDateTime yesterdayStart = today.minusDays(1).atStartOfDay();
        LocalDateTime yesterdayEnd = today.minusDays(1).atTime(LocalTime.MAX);

        long todayCount = rewardRepository.countIssuedBetween(todayStart, todayEnd);
        long yesterdayCount = rewardRepository.countIssuedBetween(yesterdayStart, yesterdayEnd);

        if (yesterdayCount == 0) {
            return todayCount == 0 ? 0.0 : 100.0;
        }

        return Math.round(((double) (todayCount - yesterdayCount) / yesterdayCount) * 1000) / 10.0;
    }

    @Transactional(readOnly = true)
    public double getCurrentDailyAverageIssued() {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.atTime(23, 59, 59);
        return (double) rewardRepository.countIssuedBetween(start, end);
    }

    @Transactional(readOnly = true)
    public double getCurrentPerUserAverageIssued() {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = LocalDate.now().atTime(23, 59, 59);
        long todayRewards = rewardRepository.countIssuedBetween(start, end);
        long todayUsers = rewardRepository.countDistinctUsersBetween(start, end);

        if (todayUsers == 0) return 0.0;
        return Math.round((double) todayRewards * 10 / todayUsers) / 10.0;
    }

    @Transactional(readOnly = true)
    public double getPerUserAverageChangeRate() {
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.atTime(23, 59, 59);
        long todayRewards = rewardRepository.countIssuedBetween(todayStart, todayEnd);
        long todayUsers = rewardRepository.countDistinctUsersBetween(todayStart, todayEnd);
        double todayAverage = todayUsers == 0 ? 0.0 : (double) todayRewards / todayUsers;

        LocalDateTime yesterdayStart = today.minusDays(1).atStartOfDay();
        LocalDateTime yesterdayEnd = today.minusDays(1).atTime(23, 59, 59);
        long yesterdayRewards = rewardRepository.countIssuedBetween(yesterdayStart, yesterdayEnd);
        long yesterdayUsers = rewardRepository.countDistinctUsersBetween(yesterdayStart, yesterdayEnd);
        double yesterdayAverage = yesterdayUsers == 0 ? 0.0 : (double) yesterdayRewards / yesterdayUsers;

        if (yesterdayAverage == 0) {
            return todayAverage == 0 ? 0.0 : 100.0;
        }

        return Math.round(((todayAverage - yesterdayAverage) / yesterdayAverage) * 1000) / 10.0;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "totalRewardStats", unless = "#result.totalRewardStatistics.isEmpty()")
    public AdminRewardDto.TotalReasonStatsResponse getTotalRewardStats() {
        try {
            List<Object[]> rawStats = rewardRepository.getCurrentYearIssuedGroupedByReason();
            long totalCount = rawStats.stream().mapToLong(r -> (long) r[1]).sum();

            List<AdminRewardDto.ReasonStat> stats = rawStats.stream()
                    .map(r -> {
                        String description = (String) r[0];
                        long count = (long) r[1];
                        RewardReason reasonEnum = RewardReason.fromDescription(description);
                        double ratio = totalCount > 0 ? Math.round((double) count / totalCount * 1000) / 10.0 : 0.0;
                        return AdminRewardDto.ReasonStat.of(reasonEnum, count, ratio);
                    })
                    .toList();

            return AdminRewardDto.TotalReasonStatsResponse.of(stats);
        } catch (Exception e) {
            log.error("Failed to get total reward stats", e);
            return AdminRewardDto.TotalReasonStatsResponse.of(Collections.emptyList());
        }
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "monthlyReasonStats", key = "#month != null ? #month : T(java.time.LocalDate).now().format(T(java.time.format.DateTimeFormatter).ofPattern('yyyy-MM'))")
    public AdminRewardDto.MonthlyReasonStatsResponse getMonthlyRewardStatsByReason(String month) {
        try {
            // month가 null이면 현재 월 사용
            String targetMonth = month != null ? month : LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

            List<Object[]> rawStats = rewardRepository.getMonthlyRewardStatsByReason(targetMonth);
            long totalCount = rawStats.stream().mapToLong(r -> (long) r[1]).sum();

            List<AdminRewardDto.ReasonStat> stats = rawStats.stream()
                    .map(r -> {
                        String description = (String) r[0];
                        long count = (long) r[1];
                        RewardReason reasonEnum = RewardReason.fromDescription(description);
                        double ratio = totalCount > 0 ? Math.round((double) count / totalCount * 1000) / 10.0 : 0.0;
                        return AdminRewardDto.ReasonStat.of(reasonEnum, count, ratio);
                    })
                    .toList();

            return AdminRewardDto.MonthlyReasonStatsResponse.of(stats);
        } catch (Exception e) {
            log.error("Failed to get monthly reward stats by reason", e);
            return AdminRewardDto.MonthlyReasonStatsResponse.of(Collections.emptyList());
        }
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "monthlyRewardTrends", unless = "#result.monthlyRewardStatistics.isEmpty()")
    public AdminRewardDto.MonthlyStatsResponse getMonthlyRewardTrends() {
        try {
            LocalDateTime startDate = LocalDate.now().minusMonths(11).withDayOfMonth(1).atStartOfDay();
            List<Object[]> rawStats = rewardRepository.findMonthlyIssuedStatsLast12Months(startDate);

            Map<String, Integer> monthlyAmounts = rawStats.stream()
                    .collect(Collectors.toMap(
                            obj -> obj[0] + "-" + String.format("%02d", obj[1]),
                            obj -> ((Number) obj[2]).intValue()
                    ));

            YearMonth current = YearMonth.now();
            List<AdminRewardDto.MonthlyRewardStat> stats = new ArrayList<>();

            for (int i = 11; i >= 0; i--) {
                YearMonth month = current.minusMonths(i);
                String key = month.getYear() + "-" + String.format("%02d", month.getMonthValue());

                stats.add(AdminRewardDto.MonthlyRewardStat.builder()
                        .year(month.getYear())
                        .month(month.getMonthValue())
                        .amount(monthlyAmounts.getOrDefault(key, 0))
                        .build());
            }

            return AdminRewardDto.MonthlyStatsResponse.of(stats);
        } catch (Exception e) {
            log.error("Failed to get monthly reward trends", e);
            return AdminRewardDto.MonthlyStatsResponse.of(Collections.emptyList());
        }
    }

    @Transactional(readOnly = true)
    public Page<AdminRewardDto.AllRewardHistoryResponse.RewardHistoryItem> getAllRewardHistory(Pageable pageable) {
        try {
            Page<Reward> rewardPage = rewardRepository.findAllByOrderByCreatedAtDesc(pageable);

            return rewardPage.map(reward -> {
                RewardReason reasonEnum = RewardReason.fromDescription(reward.getDescription());
                return AdminRewardDto.AllRewardHistoryResponse.RewardHistoryItem.builder()
                        .rewardId("SEED_" + reward.getId())
                        .issuedDate(reward.getCreatedAt().toLocalDate())
                        .reason(reasonEnum.getLabel())
                        .amount(reward.getAmount().intValue())
                        .build();
            });
        } catch (Exception e) {
            log.error("Failed to get all reward history", e);
            throw new RuntimeException("리워드 내역 조회에 실패했습니다.", e);
        }
    }

    // 🔧 수정: 필터링 메서드 - userId 기반으로 간소화
    @Transactional(readOnly = true)
    public AdminRewardDto.RewardFilterResponse filterRewards(
            String userId,
            String email,
            String description,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable) {

        try {
            // 날짜 검증
            if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
                throw new IllegalArgumentException("시작일은 종료일보다 이전이어야 합니다.");
            }

            Page<Reward> page = rewardRepository.filterRewards(
                    userId,
                    description,
                    startDate != null ? startDate.atStartOfDay() : null,
                    endDate != null ? endDate.plusDays(1).atStartOfDay().minusNanos(1) : null,
                    pageable);

            List<AdminRewardDto.FilteredReward> result = mapToFilteredRewards(page.getContent());
            return AdminRewardDto.RewardFilterResponse.of(result, page);

        } catch (Exception e) {
            log.error("Failed to filter rewards", e);
            throw new RuntimeException("리워드 필터링에 실패했습니다.", e);
        }
    }

    @Transactional(readOnly = true)
    public AdminRewardDto.RewardsByDriveResponse getRewardsByDrive(AdminRewardDto.RewardsByDriveRequest request) {
        try {
            if (CollectionUtils.isEmpty(request.getDriveIds())) {
                return AdminRewardDto.RewardsByDriveResponse.of(Collections.emptyList());
            }

            List<AdminRewardDto.DriveReward> rewards = request.getDriveIds().stream()
                    .map(driveId -> {
                        int sum = rewardRepository.sumAmountByDriveId(driveId).orElse(0);
                        return AdminRewardDto.DriveReward.builder()
                                .driveId(driveId)
                                .rewards(sum)
                                .build();
                    })
                    .toList();

            return AdminRewardDto.RewardsByDriveResponse.of(rewards);
        } catch (Exception e) {
            log.error("Failed to get rewards by drive", e);
            throw new RuntimeException("운전별 리워드 조회에 실패했습니다.", e);
        }
    }

    // 🔧 수정: 간소화된 헬퍼 메서드 - email 조회 없이 userId 직접 사용
    private List<AdminRewardDto.FilteredReward> mapToFilteredRewards(List<Reward> rewards) {
        if (CollectionUtils.isEmpty(rewards)) {
            return Collections.emptyList();
        }

        return rewards.stream()
                .map(reward -> {
                    RewardReason reasonEnum = RewardReason.fromDescription(reward.getDescription());
                    return AdminRewardDto.FilteredReward.builder()
                            .rewardId("SEED_" + reward.getId())
                            .userId(reward.getUserId()) // 🔧 email → userId로 변경
                            .issuedDate(reward.getCreatedAt())
                            .reason(reasonEnum.getLabel())
                            .amount(reward.getAmount().intValue())
                            .build();
                })
                .toList();
    }
}