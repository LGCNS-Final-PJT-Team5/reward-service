
package com.modive.rewardservice.service;

import com.modive.rewardservice.domain.*;
import com.modive.rewardservice.dto.request.RewardEarnRequest;
import com.modive.rewardservice.dto.request.ScoreInfo;
import com.modive.rewardservice.repository.RewardBalanceRepository;
import com.modive.rewardservice.repository.RewardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RewardServiceTest {

    @Mock
    private RewardBalanceRepository rewardBalanceRepository;

    @Mock
    private RewardRepository rewardRepository;

    @InjectMocks
    private RewardService rewardService;

    private final String userId = "1";
    private final Long amount = 1000L;
    private final String description = "MoBTI 향상";

    @BeforeEach
    void initMockDefaults() {
        given(rewardBalanceRepository.findByUserId(any()))
                .willReturn(Optional.of(RewardBalance.builder().userId(userId).balance(100L).build()));

        given(rewardBalanceRepository.save(any()))
                .willAnswer(invocation -> invocation.getArgument(0));

        given(rewardRepository.save(any()))
                .willAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("주행 중/후/MoBTI 보상 및 잔액/내역 전체 테스트")
    void integratedRewardTest() {
        // 주행 중 테스트
        RewardEarnRequest drivingRequest = RewardEarnRequest.builder()
                .userId(userId)
                .drivingTime(15)
                .build();
        rewardService.calculateAndEarn(drivingRequest);
        verify(rewardRepository, atLeastOnce()).save(any(Reward.class));

        // 종합 점수 테스트
        given(rewardRepository.countByUserIdAndDescriptionLikeAndDateRange(any(), any(), any(), any()))
                .willReturn(0L);
        RewardEarnRequest scoreRequest = RewardEarnRequest.builder()
                .userId(userId)
                .score(80)
                .build();
        rewardService.calculateAndEarn(scoreRequest);
        verify(rewardRepository, atLeast(2)).save(any(Reward.class)); // 주행 중 + 점수

        // MoBTI 향상 테스트
        ScoreInfo last = ScoreInfo.builder().carbon(30).safety(30).accident(30).focus(30).build();
        ScoreInfo current = ScoreInfo.builder().carbon(60).safety(60).accident(60).focus(60).build();
        RewardEarnRequest mbtiRequest = RewardEarnRequest.builder()
                .userId(userId)
                .lastScore(last)
                .currentScore(current)
                .build();
        rewardService.calculateAndEarn(mbtiRequest);
        verify(rewardRepository, atLeast(3)).save(any(Reward.class)); // 총 3회 이상

        // 잔액 조회 테스트
        RewardBalance balance = RewardBalance.builder().userId(userId).balance(1000L).build();
        given(rewardBalanceRepository.findByUserId(userId)).willReturn(Optional.of(balance));
        Long result = rewardService.getBalance(userId);
        assertThat(result).isEqualTo(1000L);

        // 히스토리 조회 테스트
        Pageable pageable = PageRequest.of(0, 10);
        List<Reward> rewards = Arrays.asList(
                Reward.builder().userId(userId).amount(amount).description("적립1").type(RewardType.EARNED).balanceSnapshot(amount).build()
        );
        Page<Reward> rewardPage = new PageImpl<>(rewards, pageable, rewards.size());
        given(rewardRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)).willReturn(rewardPage);

        Page<Reward> history = rewardService.getRewardHistory(userId, pageable);
        assertThat(history).isNotNull();
        assertThat(history.getTotalElements()).isEqualTo(1);
        assertThat(history.getContent().get(0).getDescription()).isEqualTo("적립1");
    }
}
