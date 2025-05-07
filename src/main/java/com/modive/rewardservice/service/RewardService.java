package com.modive.rewardservice.service;

import com.modive.rewardservice.dto.*;
import com.modive.rewardservice.entity.Reward;
import com.modive.rewardservice.entity.enums.RewardReason;
import com.modive.rewardservice.entity.enums.RewardType;
import com.modive.rewardservice.repository.RewardRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RewardService {

    private final RewardRepository rewardRepository;

    @Transactional
    public Reward earnReward(RewardEarnRequest request) {
        if (request.getAmount() <= 0) {
            throw new IllegalArgumentException("적립 씨앗은 0보다 커야 합니다.");
        }

        Reward reward = Reward.builder()
                .userId(request.getUserId())
                .amount(request.getAmount())
                .type(RewardType.EARN)
                .reason(request.getReason())
                .description(request.getDescription())
                .build();

        return rewardRepository.save(reward);
    }

    @Transactional
    public Reward useReward(String userId, Integer amount, RewardReason reason, String description) {
        if (amount <= 0) {
            throw new IllegalArgumentException("사용 씨앗은 0보다 커야 합니다.");
        }

        Integer balance = rewardRepository.getCurrentBalanceByUserId(userId);
        if (balance == null) balance = 0;

        if (balance < amount) {
            throw new IllegalArgumentException("사용 가능한 씨앗이 부족합니다.");
        }

        Reward reward = Reward.builder()
                .userId(userId)
                .amount(-amount)
                .type(RewardType.USE)
                .reason(reason)
                .description(description)
                .build();

        return rewardRepository.save(reward);
    }

    @Transactional(readOnly = true)
    public RewardSummaryDTO getRewardSummary(String userId) {
        Integer earnedSeeds = rewardRepository.getTotalEarnedByUserId(userId);
        if (earnedSeeds == null) earnedSeeds = 0;

        Integer usedSeeds = rewardRepository.getTotalUsedByUserId(userId);
        if (usedSeeds == null) usedSeeds = 0;

        Integer availableSeeds = earnedSeeds - usedSeeds;

        List<Reward> rewardEntities = rewardRepository.findByUserIdOrderByCreatedAtDesc(userId);
        List<RewardHistoryDTO> historyList = new ArrayList<>();
        Integer runningBalance = 0;

        for (Reward entity : rewardEntities) {
            runningBalance += entity.getAmount();

            RewardHistoryDTO dto = RewardHistoryDTO.builder()
                    .id(entity.getId())
                    .description(entity.getDescription())
                    .type(entity.getAmount() > 0 ? "적립" : "사용")
                    .amount(Math.abs(entity.getAmount()))
                    .balance(runningBalance)
                    .date(entity.getCreatedAt())
                    .build();

            historyList.add(dto);
        }

        return RewardSummaryDTO.builder()
                .availableSeeds(availableSeeds)
                .totalUsedSeeds(usedSeeds)
                .history(historyList)
                .build();
    }

    @Transactional(readOnly = true)
    public int getUserTotal(String userId) {
        Integer total = rewardRepository.getCurrentBalanceByUserId(userId);
        return total != null ? total : 0;
    }

    @Transactional(readOnly = true)
    public List<Reward> getUserHistory(String userId) {
        return rewardRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public TotalIssuedDTO getTotalIssued() {
        Integer total = rewardRepository.sumAllIssued();
        if (total == null) total = 0;
        return new TotalIssuedDTO(total, 3.2); // 추후 변화율 계산 로직 추가
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getMonthlyIssuedWithChangeRate() {
        return Map.of("value", 20700, "changeRate", 12.5);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDailyAverageIssuedWithChangeRate() {
        return Map.of("value", 730, "changeRate", 5.8);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getPerUserAverageIssuedWithChangeRate() {
        return Map.of("value", 158, "changeRate", 2.1);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getIssuedByReasonFormatted() {
        return List.of(
                Map.of("reason", "종합점수", "count", 1200, "ratio", 25),
                Map.of("reason", "주행 점수(10분)", "count", 3400, "ratio", 40)
        );
    }

    @Transactional(readOnly = true)
    public List<MonthlyRewardStatsDTO> getMonthlyStats(int year, int month) {
        return List.of(
                new MonthlyRewardStatsDTO("2025-05", 15000),
                new MonthlyRewardStatsDTO("2025-06", 18000)
        );
    }

    @Transactional(readOnly = true)
    public List<RewardHistorySimpleDTO> getAllHistorySimple(int page, int pageSize) {
        return List.of(
                new RewardHistorySimpleDTO("SEED_1024", "user1@example.com"),
                new RewardHistorySimpleDTO("SEED_1023", "user2@example.com")
        );
    }

    @Transactional(readOnly = true)
    public Reward getRewardById(Long rewardId) {
        return rewardRepository.findById(rewardId)
                .orElseThrow(() -> new EntityNotFoundException("리워드 ID를 찾을 수 없습니다: " + rewardId));
    }

    @Transactional(readOnly = true)
    public List<SearchRewardResultDTO> searchReward(int searchKeyword) {
        return List.of(new SearchRewardResultDTO("SEED_1024", "user1@example.com"));
    }

    @Transactional(readOnly = true)
    public List<Reward> getAllHistory() {
        return rewardRepository.findAll();
    }
}


