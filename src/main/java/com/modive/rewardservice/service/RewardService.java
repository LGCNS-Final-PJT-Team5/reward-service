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

        // ✅ 1. 주행 중 리워드 (주행 시간 >= 10분) - 제한 없음
        if (request.getDrivingTime() != null && request.getDrivingTime() >= 10) {
            earn(userId, 1L, "주행 중 이벤트 미감지 보상");
        }

        // ✅ 2. 종합 점수 리워드 (score ≥ 50, 하루 최대 2회까지)
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

        // ✅ 3. MoBTI 향상 리워드 (좋은 쪽으로 1개 이상 변할 때만, 하루 최대 2회까지)
        String lastMbti = determineMbtiType(request.getLastScore());
        String currentMbti = determineMbtiType(request.getCurrentScore());

        if (isMbtiImproved(lastMbti, currentMbti)) {
            LocalDateTime startOfDay = today.atStartOfDay();
            LocalDateTime endOfDay = today.atTime(23, 59, 59);

            long mbtiCountToday = rewardRepository.countByUserIdAndDescriptionLikeAndDateRange(
                    userId, "MoBTI 향상 보상%", startOfDay, endOfDay
            );

            if (mbtiCountToday < 2) {
                earn(userId, 5L, "MoBTI 향상 보상: " + lastMbti + " → " + currentMbti);
            }
        }
    }

    /**
     * 종합 점수에 따른 씨앗 보상 계산
     */
    private long calculateScoreReward(int score) {
        return switch (score / 10) {
            case 10, 9 -> 5;  // 90-100점: 5씨앗
            case 8 -> 4;      // 80-89점: 4씨앗
            case 7 -> 3;      // 70-79점: 3씨앗
            case 6 -> 2;      // 60-69점: 2씨앗
            case 5 -> 1;      // 50-59점: 1씨앗
            default -> 0;     // 0-49점: 없음
        };
    }

    /**
     * ScoreInfo를 기반으로 MoBTI 타입 결정
     * 51점 이상이면 좋은 타입(E/D/S/F), 미만이면 나쁜 타입(H/A/I/U)
     */
    private String determineMbtiType(ScoreInfo score) {
        if (score == null) return null;

        String eco = score.getCarbon() != null && score.getCarbon() >= 51 ? "E" : "H";     // 에코 vs 헤비
        String safety = score.getSafety() != null && score.getSafety() >= 51 ? "D" : "A";  // 방어 vs 공격
        String accident = score.getAccident() != null && score.getAccident() >= 51 ? "S" : "I"; // 민감 vs 둔감
        String focus = score.getFocus() != null && score.getFocus() >= 51 ? "F" : "U";    // 집중 vs 산만

        return eco + safety + accident + focus;
    }

    /**
     * MoBTI 향상 여부 판정
     * 좋은 타입: E, D, S, F
     * 하나 이상이 좋은 쪽으로 변하면 향상으로 간주
     */
    private boolean isMbtiImproved(String lastMbti, String currentMbti) {
        if (lastMbti == null || currentMbti == null || lastMbti.equals(currentMbti)) {
            return false;
        }

        // 각 자리별로 향상 여부 체크
        char[] lastChars = lastMbti.toCharArray();
        char[] currentChars = currentMbti.toCharArray();

        // 하나라도 좋은 쪽으로 변했는지 확인
        for (int i = 0; i < 4; i++) {
            if (isImprovedAtPosition(lastChars[i], currentChars[i], i)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 각 포지션별 향상 여부 체크
     * @param lastChar 이전 타입
     * @param currentChar 현재 타입
     * @param position 0:에너지관리, 1:주행스타일, 2:반응민감도, 3:주의집중도
     */
    private boolean isImprovedAtPosition(char lastChar, char currentChar, int position) {
        return switch (position) {
            case 0 -> lastChar == 'H' && currentChar == 'E';  // Heavy → Eco
            case 1 -> lastChar == 'A' && currentChar == 'D';  // Aggressive → Defensive
            case 2 -> lastChar == 'I' && currentChar == 'S';  // Insensitive → Sensitive
            case 3 -> lastChar == 'U' && currentChar == 'F';  // Unfocused → Focused
            default -> false;
        };
    }

    /**
     * 씨앗 적립 처리
     */
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
    public Long getBalance(Long userId) {
        return rewardBalanceRepository.findByUserId(userId)
                .map(RewardBalance::getBalance)
                .orElse(0L);
    }

    /**사용자별 씨앗 적립 내역 페이징 조회**/
    @Transactional(readOnly = true)
    public Page<Reward> getRewardHistory(Long userId, Pageable pageable) {
        return rewardRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }
}