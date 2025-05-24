package com.modive.rewardservice.service;

import com.modive.rewardservice.client.UserClient;
import com.modive.rewardservice.domain.Reward;
import com.modive.rewardservice.dto.*;
import com.modive.rewardservice.repository.RewardRepository;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

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
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneMonthAgo = now.minusMonths(1);
        long totalUntilOneMonthAgo = rewardRepository.countIssuedBefore(oneMonthAgo);

        LocalDateTime twoMonthsAgo = now.minusMonths(2);
        long totalUntilTwoMonthsAgo = rewardRepository.countIssuedBefore(twoMonthsAgo);

        if (totalUntilTwoMonthsAgo == 0) {
            return totalUntilOneMonthAgo == 0 ? 0.0 : 100.0;
        }

        return Math.round(((double) (totalUntilOneMonthAgo - totalUntilTwoMonthsAgo) / totalUntilTwoMonthsAgo) * 1000) / 10.0;
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

    /**
     * 어제 대비 오늘의 리워드 발급 건수 변화율을 계산
     */
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
    /**
     * 5. 발급 사유별 총 통계 (올해 데이터만)
     * - 종합점수, 이벤트미발생, MoBTI향상 3가지로 분류
     */
    @Transactional(readOnly = true)
    public List<AdminRewardDto.TotalReasonStatsResponse.ReasonStat> getTotalIssuedByReason() {
        List<Object[]> rawStats = rewardRepository.getCurrentYearIssuedGroupedByReason();

        // 카테고리 맵핑
        Map<String, Long> categoryMap = new HashMap<>();
        categoryMap.put("종합점수", 0L);
        categoryMap.put("이벤트미발생", 0L);
        categoryMap.put("MoBTI향상", 0L);

        long total = 0;

        for (Object[] row : rawStats) {
            String description = (String) row[0];
            long count = ((Number) row[1]).longValue();
            total += count;

            // 발급 사유를 3가지 카테고리로 분류
            if (description.contains("종합") || description.contains("점수")) {
                categoryMap.put("종합점수", categoryMap.get("종합점수") + count);
            } else if (description.contains("이벤트") || description.contains("미발생") || description.contains("미감지")) {
                categoryMap.put("이벤트미발생", categoryMap.get("이벤트미발생") + count);
            } else if (description.contains("MoBTI") || description.contains("향상")) {
                categoryMap.put("MoBTI향상", categoryMap.get("MoBTI향상") + count);
            }
        }

        final long finalTotal = total;

        return categoryMap.entrySet().stream()
                .map(entry -> {
                    String reason = entry.getKey();
                    long count = entry.getValue();
                    double ratio = finalTotal == 0 ? 0.0 : Math.round(((double) count / finalTotal) * 1000) / 10.0;
                    return AdminRewardDto.TotalReasonStatsResponse.ReasonStat.builder()
                            .reason(reason)
                            .count(count)
                            .ratio(ratio)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 6. 월별 씨앗 지급 통계 (최근 12개월)
     */
    @Transactional(readOnly = true)
    public AdminRewardDto.MonthlyStatsResponse getMonthlyRewardStats() {
        LocalDateTime startDate = LocalDate.now().minusMonths(11).withDayOfMonth(1).atStartOfDay();
        List<Object[]> rawStats = rewardRepository.findMonthlyIssuedStatsLast12Months(startDate);

        // 최근 12개월 데이터 초기화
        List<AdminRewardDto.MonthlyRewardStat> stats = new ArrayList<>();
        YearMonth current = YearMonth.now();

        for (int i = 11; i >= 0; i--) {
            YearMonth month = current.minusMonths(i);
            stats.add(AdminRewardDto.MonthlyRewardStat.builder()
                    .year(month.getYear())
                    .month(month.getMonthValue())
                    .amount(0)
                    .build());
        }

        // 실제 데이터로 업데이트
        for (Object[] obj : rawStats) {
            int year = (Integer) obj[0];
            int month = (Integer) obj[1];
            int amount = ((Number) obj[2]).intValue();

            // 해당하는 월 찾아서 업데이트
            for (AdminRewardDto.MonthlyRewardStat stat : stats) {
                if (stat.getYear() == year && stat.getMonth() == month) {
                    stat.setAmount(amount);
                    break;
                }
            }
        }

        return AdminRewardDto.MonthlyStatsResponse.of(stats);
    }

    /**
     * 7. 최근 씨앗 발급 내역
     */
    @Transactional(readOnly = true)
    public Page<AdminRewardDto.AllRewardHistoryResponse.RewardHistoryItem> getAllRewardHistory(Pageable pageable) {
        Page<Reward> rewardPage = rewardRepository.findAllByOrderByCreatedAtDesc(pageable);
        return rewardPage.map(AdminRewardDto.AllRewardHistoryResponse.RewardHistoryItem::from);
    }

    /**
     * 8. 씨앗 필터링 (이메일, 발급일, 발급사유)
     */
    @Transactional(readOnly = true)
    public AdminRewardDto.RewardFilterResponse filterRewards(
            String email,
            String description,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    ) {
        Long userId = null;
        if (email != null && !email.isBlank()) {
            userId = userClient.getUserIdByEmail(email);
        }

        Page<Reward> page = rewardRepository.filterRewards(
                userId,
                description,
                startDate != null ? startDate.atStartOfDay() : null,
                endDate != null ? endDate.plusDays(1).atStartOfDay().minusNanos(1) : null,
                pageable
        );

        List<AdminRewardDto.FilteredReward> result = page.getContent().stream()
                .map(reward -> {
                    String userEmail = userClient.getEmailByUserId(reward.getUserId());
                    return AdminRewardDto.FilteredReward.builder()
                            .rewardId("SEED_" + reward.getId())
                            .email(userEmail)
                            .createdAt(reward.getCreatedAt())
                            .description(reward.getDescription())
                            .amount(reward.getAmount().intValue())
                            .build();
                })
                .collect(Collectors.toList());

        return AdminRewardDto.RewardFilterResponse.of(result, page);
    }

    /**
     * 9. 운전별 씨앗 적립 내역
     */
    @Transactional(readOnly = true)
    public AdminRewardDto.RewardsByDriveResponse getRewardsByDrive(AdminRewardDto.RewardsByDriveRequest request) {
        List<AdminRewardDto.DriveReward> rewards = request.getDriveIds().stream()
                .map(driveId -> {
                    int sum = rewardRepository.sumAmountByDriveId(driveId).orElse(0);
                    return AdminRewardDto.DriveReward.builder()
                            .driveId(driveId)
                            .rewards(sum)
                            .build();
                })
                .collect(Collectors.toList());

        return AdminRewardDto.RewardsByDriveResponse.of(rewards);
    }
}