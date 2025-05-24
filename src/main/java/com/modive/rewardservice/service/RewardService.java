package com.modive.rewardservice.service;

import com.modive.rewardservice.domain.RewardBalance;
import com.modive.rewardservice.domain.RewardType;
import com.modive.rewardservice.domain.*;
import com.modive.rewardservice.dto.request.RewardEarnRequest;
import com.modive.rewardservice.dto.request.ScoreInfo;
import com.modive.rewardservice.repository.RewardBalanceRepository;
import com.modive.rewardservice.repository.RewardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;


@Service
@RequiredArgsConstructor
public class RewardService {

    private final RewardRepository rewardRepository;
    private final RewardBalanceRepository rewardBalanceRepository;

    /**씨앗적립처리**/
    @Transactional
    public void calculateAndEarn(RewardEarnRequest request) {
        Long userId = request.getUserId();
        LocalDate today = LocalDate.now();

        // ✅ 1. 주행 중 리워드 (주행 시간 >= 10분)
        if (request.getDrivingTime() != null && request.getDrivingTime() >= 10) {
            earn(userId, 1L, "주행 중 이벤트 미감지 보상");
        }

        // ✅ 2. 종합 점수 리워드 (score ≥ 50, 하루 2회까지)
        if (request.getScore() != null && request.getScore() >= 50) {
            LocalDateTime startOfDay = today.atStartOfDay();
            LocalDateTime endOfDay = today.atTime(23, 59, 59);

            long countToday = rewardRepository.countByUserIdAndDescriptionLikeAndDateRange(
                    userId, "종합 점수 보상%", startOfDay, endOfDay
            );

            if (countToday < 2) {
                long seed = calculateScoreReward(request.getScore());
                if (seed > 0) {
                    earn(userId, seed, "종합 점수 보상: " + request.getScore() + "점");
                }
            }
        }

        // ✅ 3. MoBTI 향상 리워드
        String lastMbti = determineMbtiType(request.getLastScore());
        String currentMbti = determineMbtiType(request.getCurrentScore());

        if (lastMbti != null && currentMbti != null && !lastMbti.equals(currentMbti)) {
            earn(userId, 5L, "MoBTI 향상 보상: " + lastMbti + " → " + currentMbti);
        }
    }

    private long calculateScoreReward(int score) {
        return switch (score / 10) {
            case 10, 9 -> 5;
            case 8 -> 4;
            case 7 -> 3;
            case 6 -> 2;
            case 5 -> 1;
            default -> 0;
        };
    }

    private String determineMbtiType(ScoreInfo score) {
        if (score == null) return null;

        String eco = score.getCarbon() != null && score.getCarbon() >= 51 ? "E" : "H";       // 에코 vs 헤비
        String safety = score.getSafety() != null && score.getSafety() >= 51 ? "A" : "B";    // 공격 vs 방어
        String accident = score.getAccident() != null && score.getAccident() >= 51 ? "M" : "D"; // 민감 vs 둔감
        String focus = score.getFocus() != null && score.getFocus() >= 51 ? "C" : "S";       // 집중 vs 산만

        return eco + safety + accident + focus;
    }

    private Reward earn(Long userId, Long amount, String description) {
        RewardBalance rewardBalance = rewardBalanceRepository.findByUserId(userId)
                .orElseGet(() -> RewardBalance.builder()
                        .userId(userId)
                        .balance(0L)
                        .build());

        rewardBalance.addBalance(amount);
        rewardBalance = rewardBalanceRepository.save(rewardBalance);

        Reward reward = Reward.builder()
                .userId(userId)
                .amount(amount)
                .type(RewardType.EARNED)
                .description(description)
                .balanceSnapshot(rewardBalance.getBalance())
                .rewardBalance(rewardBalance)
                .build();

        return rewardRepository.save(reward);
    }


    /**사용자 현재 씨앗 잔액 조회**/
    @Transactional(readOnly = true)
    public Long getBalance(Long userId) { //현재잔액 확인
        return rewardBalanceRepository.findByUserId(userId)
                .map(RewardBalance::getBalance)
                .orElse(0L);
    }

    /**사용자별 씨앗 적립 내영 페이징 조회**/
    @Transactional(readOnly = true)
    public Page<Reward> getRewardHistory(Long userId, Pageable pageable) {
        return rewardRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }
}


