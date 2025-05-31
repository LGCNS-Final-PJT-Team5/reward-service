package com.modive.rewardservice.service;

import com.modive.rewardservice.domain.Reward;
import com.modive.rewardservice.domain.RewardBalance;
import com.modive.rewardservice.domain.RewardType;
import com.modive.rewardservice.dto.request.RewardEarnRequest;
import com.modive.rewardservice.dto.request.ScoreInfo;
import com.modive.rewardservice.repository.RewardBalanceRepository;
import com.modive.rewardservice.repository.RewardRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RewardService 단위 테스트
 * 리워드 계산 로직의 핵심 비즈니스 규칙을 검증
 */
@ExtendWith(MockitoExtension.class)
class RewardServiceTest {

    @Mock
    private RewardRepository rewardRepository;

    @Mock
    private RewardBalanceRepository rewardBalanceRepository;

    @InjectMocks
    private RewardService rewardService;

    private static final String TEST_USER_ID = "user123";

    @Test
    @DisplayName("주행시간 10분 이상 - 이벤트미발생 리워드 1씨앗 적립")
    void calculateAndEarn_DrivingTime10MinutesOrMore_EarnsDrivingReward() {
        // Given
        RewardEarnRequest request = createBaseRequest()
                .drivingTime(15) // 10분 이상
                .score(30) // 50점 미만 (점수 리워드 없음)
                .build();

        RewardBalance mockBalance = createMockBalance(100L);
        when(rewardBalanceRepository.findByUserId(TEST_USER_ID)).thenReturn(Optional.of(mockBalance));
        when(rewardBalanceRepository.save(any())).thenReturn(mockBalance);

        // When
        rewardService.calculateAndEarn(request);

        // Then
        ArgumentCaptor<Reward> rewardCaptor = ArgumentCaptor.forClass(Reward.class);
        verify(rewardRepository, times(1)).save(rewardCaptor.capture());

        Reward savedReward = rewardCaptor.getValue();
        assertThat(savedReward.getAmount()).isEqualTo(1L);
        assertThat(savedReward.getDescription()).isEqualTo("이벤트미발생");
        assertThat(savedReward.getType()).isEqualTo(RewardType.EARNED);

        verify(rewardBalanceRepository, times(1)).save(mockBalance);
        assertThat(mockBalance.getBalance()).isEqualTo(101L); // 100 + 1
    }

    @Test
    @DisplayName("주행시간 10분 미만 - 주행 리워드 없음")
    void calculateAndEarn_DrivingTimeLessThan10Minutes_NoDrivingReward() {
        // Given
        RewardEarnRequest request = createBaseRequest()
                .drivingTime(5) // 10분 미만
                .score(30) // 50점 미만
                .build();

        // When
        rewardService.calculateAndEarn(request);

        // Then
        verify(rewardRepository, never()).save(any());
        verify(rewardBalanceRepository, never()).save(any());
    }

    @Test
    @DisplayName("종합점수 리워드 - 점수별 씨앗 계산")
    void calculateAndEarn_ScoreReward_CalculatesCorrectSeeds() {
        // 각 점수 케이스를 개별적으로 테스트
        testScoreReward_90점_5씨앗();
        testScoreReward_85점_4씨앗();
        testScoreReward_75점_3씨앗();
        testScoreReward_65점_2씨앗();
        testScoreReward_55점_1씨앗();
        testScoreReward_45점_0씨앗();
    }

    private void testScoreReward_90점_5씨앗() {
        // Given
        reset(rewardRepository, rewardBalanceRepository);
        RewardEarnRequest request = createBaseRequest().score(95).drivingTime(5).build();

        when(rewardRepository.countByUserIdAndDescriptionLikeAndDateRange(
                eq(TEST_USER_ID), eq("종합점수%"), any(), any())).thenReturn(0L);
        RewardBalance mockBalance = createMockBalance(100L);
        when(rewardBalanceRepository.findByUserId(TEST_USER_ID)).thenReturn(Optional.of(mockBalance));
        when(rewardBalanceRepository.save(any())).thenReturn(mockBalance);

        // When
        rewardService.calculateAndEarn(request);

        // Then
        ArgumentCaptor<Reward> rewardCaptor = ArgumentCaptor.forClass(Reward.class);
        verify(rewardRepository).save(rewardCaptor.capture());
        assertThat(rewardCaptor.getValue().getAmount()).isEqualTo(5L);
    }

    private void testScoreReward_85점_4씨앗() {
        // Given
        reset(rewardRepository, rewardBalanceRepository);
        RewardEarnRequest request = createBaseRequest().score(85).drivingTime(5).build();

        when(rewardRepository.countByUserIdAndDescriptionLikeAndDateRange(
                eq(TEST_USER_ID), eq("종합점수%"), any(), any())).thenReturn(0L);
        RewardBalance mockBalance = createMockBalance(100L);
        when(rewardBalanceRepository.findByUserId(TEST_USER_ID)).thenReturn(Optional.of(mockBalance));
        when(rewardBalanceRepository.save(any())).thenReturn(mockBalance);

        // When
        rewardService.calculateAndEarn(request);

        // Then
        ArgumentCaptor<Reward> rewardCaptor = ArgumentCaptor.forClass(Reward.class);
        verify(rewardRepository).save(rewardCaptor.capture());
        assertThat(rewardCaptor.getValue().getAmount()).isEqualTo(4L);
    }

    private void testScoreReward_75점_3씨앗() {
        // Given
        reset(rewardRepository, rewardBalanceRepository);
        RewardEarnRequest request = createBaseRequest().score(75).drivingTime(5).build();

        when(rewardRepository.countByUserIdAndDescriptionLikeAndDateRange(
                eq(TEST_USER_ID), eq("종합점수%"), any(), any())).thenReturn(0L);
        RewardBalance mockBalance = createMockBalance(100L);
        when(rewardBalanceRepository.findByUserId(TEST_USER_ID)).thenReturn(Optional.of(mockBalance));
        when(rewardBalanceRepository.save(any())).thenReturn(mockBalance);

        // When
        rewardService.calculateAndEarn(request);

        // Then
        ArgumentCaptor<Reward> rewardCaptor = ArgumentCaptor.forClass(Reward.class);
        verify(rewardRepository).save(rewardCaptor.capture());
        assertThat(rewardCaptor.getValue().getAmount()).isEqualTo(3L);
    }

    private void testScoreReward_65점_2씨앗() {
        // Given
        reset(rewardRepository, rewardBalanceRepository);
        RewardEarnRequest request = createBaseRequest().score(65).drivingTime(5).build();

        when(rewardRepository.countByUserIdAndDescriptionLikeAndDateRange(
                eq(TEST_USER_ID), eq("종합점수%"), any(), any())).thenReturn(0L);
        RewardBalance mockBalance = createMockBalance(100L);
        when(rewardBalanceRepository.findByUserId(TEST_USER_ID)).thenReturn(Optional.of(mockBalance));
        when(rewardBalanceRepository.save(any())).thenReturn(mockBalance);

        // When
        rewardService.calculateAndEarn(request);

        // Then
        ArgumentCaptor<Reward> rewardCaptor = ArgumentCaptor.forClass(Reward.class);
        verify(rewardRepository).save(rewardCaptor.capture());
        assertThat(rewardCaptor.getValue().getAmount()).isEqualTo(2L);
    }

    private void testScoreReward_55점_1씨앗() {
        // Given
        reset(rewardRepository, rewardBalanceRepository);
        RewardEarnRequest request = createBaseRequest().score(55).drivingTime(5).build();

        when(rewardRepository.countByUserIdAndDescriptionLikeAndDateRange(
                eq(TEST_USER_ID), eq("종합점수%"), any(), any())).thenReturn(0L);
        RewardBalance mockBalance = createMockBalance(100L);
        when(rewardBalanceRepository.findByUserId(TEST_USER_ID)).thenReturn(Optional.of(mockBalance));
        when(rewardBalanceRepository.save(any())).thenReturn(mockBalance);

        // When
        rewardService.calculateAndEarn(request);

        // Then
        ArgumentCaptor<Reward> rewardCaptor = ArgumentCaptor.forClass(Reward.class);
        verify(rewardRepository).save(rewardCaptor.capture());
        assertThat(rewardCaptor.getValue().getAmount()).isEqualTo(1L);
    }

    private void testScoreReward_45점_0씨앗() {
        // Given
        reset(rewardRepository, rewardBalanceRepository);
        RewardEarnRequest request = createBaseRequest().score(45).drivingTime(5).build();

        // When
        rewardService.calculateAndEarn(request);

        // Then
        verify(rewardRepository, never()).save(any());
        verify(rewardBalanceRepository, never()).save(any());
    }

    @Test
    @DisplayName("종합점수 리워드 - 하루 최대 2회 제한")
    void calculateAndEarn_ScoreReward_LimitedTo2PerDay() {
        // Given
        RewardEarnRequest request = createBaseRequest()
                .score(85) // 4씨앗 받을 점수
                .build();

        // 이미 오늘 2번 받았다고 가정
        when(rewardRepository.countByUserIdAndDescriptionLikeAndDateRange(
                eq(TEST_USER_ID), eq("종합점수%"), any(), any())).thenReturn(2L);

        // When
        rewardService.calculateAndEarn(request);

        // Then
        verify(rewardRepository, never()).save(any()); // 저장되지 않음
    }

    @Test
    @DisplayName("MoBTI 향상 리워드 - HAIU → EDSF 변화시 5씨앗")
    void calculateAndEarn_MbtiImprovement_AllCategoriesImproved() {
        // Given
        ScoreInfo lastScore = ScoreInfo.builder()
                .carbon(40) // H (Heavy)
                .safety(40) // A (Aggressive)
                .accident(40) // I (Insensitive)
                .focus(40) // U (Unfocused)
                .build();

        ScoreInfo currentScore = ScoreInfo.builder()
                .carbon(60) // E (Eco)
                .safety(60) // D (Defensive)
                .accident(60) // S (Sensitive)
                .focus(60) // F (Focused)
                .build();

        RewardEarnRequest request = createBaseRequest()
                .lastScore(lastScore)
                .currentScore(currentScore)
                .score(30) // 점수 리워드 없음
                .drivingTime(5) // 주행 리워드 없음
                .build();

        when(rewardRepository.countByUserIdAndDescriptionLikeAndDateRange(
                eq(TEST_USER_ID), eq("MoBTI향상%"), any(), any())).thenReturn(0L);

        RewardBalance mockBalance = createMockBalance(100L);
        when(rewardBalanceRepository.findByUserId(TEST_USER_ID)).thenReturn(Optional.of(mockBalance));
        when(rewardBalanceRepository.save(any())).thenReturn(mockBalance);

        // When
        rewardService.calculateAndEarn(request);

        // Then
        ArgumentCaptor<Reward> rewardCaptor = ArgumentCaptor.forClass(Reward.class);
        verify(rewardRepository).save(rewardCaptor.capture());

        Reward savedReward = rewardCaptor.getValue();
        assertThat(savedReward.getAmount()).isEqualTo(5L);
        assertThat(savedReward.getDescription()).isEqualTo("MoBTI향상");
    }

    @Test
    @DisplayName("MoBTI 향상 리워드 - 일부만 향상되어도 5씨앗")
    void calculateAndEarn_MbtiImprovement_PartialImprovement() {
        // Given - Carbon만 향상 (H → E)
        ScoreInfo lastScore = ScoreInfo.builder()
                .carbon(40) // H → E 향상
                .safety(60) // D 유지
                .accident(60) // S 유지
                .focus(60) // F 유지
                .build();

        ScoreInfo currentScore = ScoreInfo.builder()
                .carbon(60) // E
                .safety(60) // D
                .accident(60) // S
                .focus(60) // F
                .build();

        RewardEarnRequest request = createBaseRequest()
                .lastScore(lastScore)
                .currentScore(currentScore)
                .build();

        when(rewardRepository.countByUserIdAndDescriptionLikeAndDateRange(
                eq(TEST_USER_ID), eq("MoBTI향상%"), any(), any())).thenReturn(0L);

        RewardBalance mockBalance = createMockBalance(100L);
        when(rewardBalanceRepository.findByUserId(TEST_USER_ID)).thenReturn(Optional.of(mockBalance));
        when(rewardBalanceRepository.save(any())).thenReturn(mockBalance);

        // When
        rewardService.calculateAndEarn(request);

        // Then
        verify(rewardRepository).save(any());
    }

    @Test
    @DisplayName("MoBTI 변화 없음 - 리워드 없음")
    void calculateAndEarn_NoMbtiChange_NoReward() {
        // Given - 점수는 같음 (EDSF → EDSF)
        ScoreInfo sameScore = ScoreInfo.builder()
                .carbon(60).safety(60).accident(60).focus(60).build();

        RewardEarnRequest request = createBaseRequest()
                .lastScore(sameScore)
                .currentScore(sameScore)
                .build();

        // When
        rewardService.calculateAndEarn(request);

        // Then
        verify(rewardRepository, never()).save(any());
    }

    @Test
    @DisplayName("MoBTI 향상 리워드 - 하루 최대 2회 제한")
    void calculateAndEarn_MbtiImprovement_LimitedTo2PerDay() {
        // Given
        ScoreInfo lastScore = ScoreInfo.builder().carbon(40).safety(60).accident(60).focus(60).build();
        ScoreInfo currentScore = ScoreInfo.builder().carbon(60).safety(60).accident(60).focus(60).build();

        RewardEarnRequest request = createBaseRequest()
                .lastScore(lastScore)
                .currentScore(currentScore)
                .build();

        // 이미 오늘 2번 받았다고 가정
        when(rewardRepository.countByUserIdAndDescriptionLikeAndDateRange(
                eq(TEST_USER_ID), eq("MoBTI향상%"), any(), any())).thenReturn(2L);

        // When
        rewardService.calculateAndEarn(request);

        // Then
        verify(rewardRepository, never()).save(any());
    }

    @Test
    @DisplayName("복합 리워드 - 주행 + 점수 + MoBTI 모두 적립")
    void calculateAndEarn_MultipleRewards_AllEarned() {
        // Given
        ScoreInfo lastScore = ScoreInfo.builder().carbon(40).safety(60).accident(60).focus(60).build();
        ScoreInfo currentScore = ScoreInfo.builder().carbon(60).safety(60).accident(60).focus(60).build();

        RewardEarnRequest request = createBaseRequest()
                .drivingTime(15) // 주행 리워드
                .score(85) // 점수 리워드 4씨앗
                .lastScore(lastScore)
                .currentScore(currentScore) // MoBTI 향상 리워드
                .build();

        when(rewardRepository.countByUserIdAndDescriptionLikeAndDateRange(
                eq(TEST_USER_ID), anyString(), any(), any())).thenReturn(0L);

        RewardBalance mockBalance = createMockBalance(100L);
        when(rewardBalanceRepository.findByUserId(TEST_USER_ID)).thenReturn(Optional.of(mockBalance));
        when(rewardBalanceRepository.save(any())).thenReturn(mockBalance);

        // When
        rewardService.calculateAndEarn(request);

        // Then
        verify(rewardRepository, times(3)).save(any()); // 3개 리워드 저장
        verify(rewardBalanceRepository, times(3)).save(any()); // 3번 잔액 업데이트
        assertThat(mockBalance.getBalance()).isEqualTo(110L); // 100 + 1 + 4 + 5
    }

    @Test
    @DisplayName("새 사용자 - RewardBalance 생성")
    void calculateAndEarn_NewUser_CreatesRewardBalance() {
        // Given
        RewardEarnRequest request = createBaseRequest()
                .drivingTime(15)
                .build();

        when(rewardBalanceRepository.findByUserId(TEST_USER_ID)).thenReturn(Optional.empty());

        RewardBalance newBalance = RewardBalance.builder().userId(TEST_USER_ID).balance(0L).build();
        when(rewardBalanceRepository.save(any())).thenReturn(newBalance);

        // When
        rewardService.calculateAndEarn(request);

        // Then
        ArgumentCaptor<RewardBalance> balanceCaptor = ArgumentCaptor.forClass(RewardBalance.class);
        verify(rewardBalanceRepository).save(balanceCaptor.capture());

        RewardBalance savedBalance = balanceCaptor.getValue();
        assertThat(savedBalance.getUserId()).isEqualTo(TEST_USER_ID);
        assertThat(savedBalance.getBalance()).isEqualTo(1L); // 0 + 1
    }

    @Test
    @DisplayName("잔액 조회 - 기존 사용자")
    void getBalance_ExistingUser_ReturnsBalance() {
        // Given
        Long expectedBalance = 150L;
        RewardBalance balance = createMockBalance(expectedBalance);
        when(rewardBalanceRepository.findByUserId(TEST_USER_ID)).thenReturn(Optional.of(balance));

        // When
        Long actualBalance = rewardService.getBalance(TEST_USER_ID);

        // Then
        assertThat(actualBalance).isEqualTo(expectedBalance);
    }

    @Test
    @DisplayName("잔액 조회 - 신규 사용자는 0 반환")
    void getBalance_NewUser_ReturnsZero() {
        // Given
        when(rewardBalanceRepository.findByUserId(TEST_USER_ID)).thenReturn(Optional.empty());

        // When
        Long actualBalance = rewardService.getBalance(TEST_USER_ID);

        // Then
        assertThat(actualBalance).isEqualTo(0L);
    }

    @Test
    @DisplayName("리워드 내역 조회 - 페이징")
    void getRewardHistory_WithPaging_ReturnsPagedResults() {
        // Given
        List<Reward> rewards = Arrays.asList(
                createMockReward(1L, 5L, "종합점수"),
                createMockReward(2L, 1L, "이벤트미발생")
        );

        Pageable pageable = PageRequest.of(0, 10);
        Page<Reward> expectedPage = new PageImpl<>(rewards, pageable, 2);

        when(rewardRepository.findByUserIdOrderByCreatedAtDesc(TEST_USER_ID, pageable))
                .thenReturn(expectedPage);

        // When
        Page<Reward> actualPage = rewardService.getRewardHistory(TEST_USER_ID, pageable);

        // Then
        assertThat(actualPage.getContent()).hasSize(2);
        assertThat(actualPage.getTotalElements()).isEqualTo(2);
        assertThat(actualPage.getContent().get(0).getAmount()).isEqualTo(5L);
        assertThat(actualPage.getContent().get(1).getAmount()).isEqualTo(1L);
    }

    // ===== Helper Methods =====

    private RewardEarnRequest.RewardEarnRequestBuilder createBaseRequest() {
        return RewardEarnRequest.builder()
                .userId(TEST_USER_ID)
                .driveId("drive123");
    }

    private RewardBalance createMockBalance(Long balance) {
        return RewardBalance.builder()
                .userId(TEST_USER_ID)
                .balance(balance)
                .build();
    }

    private Reward createMockReward(Long id, Long amount, String description) {
        return Reward.builder()
                .userId(TEST_USER_ID)
                .amount(amount)
                .type(RewardType.EARNED)
                .description(description)
                .balanceSnapshot(100L + amount)
                .build();
    }
}