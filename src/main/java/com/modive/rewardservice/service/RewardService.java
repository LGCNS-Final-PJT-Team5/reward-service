package com.modive.rewardservice.service;

import com.modive.rewardservice.dto.RewardHistoryDTO;
import com.modive.rewardservice.dto.RewardSummaryDTO;
import com.modive.rewardservice.entity.Reward;
import com.modive.rewardservice.repository.RewardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RewardService {

    private final RewardRepository rewardRepository;

    @Transactional
    public Reward earnReward(String userId, Integer amount, String reason, String description) {
        if (amount <= 0) {
            throw new IllegalArgumentException("적립 씨앗은 0보다 커야 합니다.");
        }

        Reward reward = Reward.builder()
                .userId(userId)
                .amount(amount)
                .type("EARN")
                .reason(reason)
                .description(description)
                .build();

        return rewardRepository.save(reward);
    }

    @Transactional
    public Reward useReward(String userId, Integer amount, String reason, String description) {
        if (amount <= 0) {
            throw new IllegalArgumentException("사용 씨앗은 0보다 커야 합니다.");
        }

        // 현재 잔액 확인
        Integer balance = rewardRepository.getCurrentBalanceByUserId(userId);
        if (balance == null) {
            balance = 0;
        }

        if (balance < amount) {
            throw new IllegalArgumentException("사용 가능한 씨앗이 부족합니다.");
        }

        Reward reward = Reward.builder()
                .userId(userId)
                .amount(-amount) // 음수로 저장
                .type("USE")
                .reason(reason)
                .description(description)
                .build();

        return rewardRepository.save(reward);
    }

    @Transactional(readOnly = true)
    public RewardSummaryDTO getRewardSummary(String userId) {
        // 총 적립 씨앗
        Integer earnedSeeds = rewardRepository.getTotalEarnedByUserId(userId);
        if (earnedSeeds == null) {
            earnedSeeds = 0;
        }

        // 총 사용 씨앗
        Integer usedSeeds = rewardRepository.getTotalUsedByUserId(userId);
        if (usedSeeds == null) {
            usedSeeds = 0;
        }

        // 사용 가능 씨앗 = 총 적립 - 총 사용
        Integer availableSeeds = earnedSeeds - usedSeeds;

        // 씨앗 적립/사용 내역
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
}