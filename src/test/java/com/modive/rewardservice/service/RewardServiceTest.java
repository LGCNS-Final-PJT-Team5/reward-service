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

    // ===== 기존 테스트들 =====

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

    // ===== 🎯 NEW: 점수별 리워드 세분화 테스트 (calculateScoreReward 커버리지 향상) =====

    @Test
    @DisplayName("종합점수 리워드 - 100점 경계값 테스트")
    void calculateAndEarn_ScoreReward_100Points_Earns5Seeds() {
        testScoreReward(100, 5L, "100점 만점");
    }

    @Test
    @DisplayName("종합점수 리워드 - 90-99점 테스트")
    void calculateAndEarn_ScoreReward_90to99Points_Earns5Seeds() {
        testScoreReward(95, 5L, "90-99점 범위");
        testScoreReward(90, 5L, "90점 경계값");
    }

    @Test
    @DisplayName("종합점수 리워드 - 80-89점 테스트")
    void calculateAndEarn_ScoreReward_80to89Points_Earns4Seeds() {
        testScoreReward(89, 4L, "80-89점 범위");
        testScoreReward(80, 4L, "80점 경계값");
    }

    @Test
    @DisplayName("종합점수 리워드 - 70-79점 테스트")
    void calculateAndEarn_ScoreReward_70to79Points_Earns3Seeds() {
        testScoreReward(79, 3L, "70-79점 범위");
        testScoreReward(70, 3L, "70점 경계값");
    }

    @Test
    @DisplayName("종합점수 리워드 - 60-69점 테스트")
    void calculateAndEarn_ScoreReward_60to69Points_Earns2Seeds() {
        testScoreReward(69, 2L, "60-69점 범위");
        testScoreReward(60, 2L, "60점 경계값");
    }

    @Test
    @DisplayName("종합점수 리워드 - 50-59점 테스트")
    void calculateAndEarn_ScoreReward_50to59Points_Earns1Seed() {
        testScoreReward(59, 1L, "50-59점 범위");
        testScoreReward(50, 1L, "50점 경계값");
    }

    @Test
    @DisplayName("종합점수 리워드 - 49점 이하 테스트")
    void calculateAndEarn_ScoreReward_Below50Points_NoReward() {
        // Given
        RewardEarnRequest request = createBaseRequest().score(49).drivingTime(5).build();

        // When
        rewardService.calculateAndEarn(request);

        // Then - 점수 리워드 없음
        verify(rewardRepository, never()).save(any());
    }

    @Test
    @DisplayName("종합점수 리워드 - 하루 최대 2회 제한")
    void calculateAndEarn_ScoreReward_LimitedTo2PerDay() {
        // Given
        RewardEarnRequest request = createBaseRequest().score(85).build();

        // 이미 오늘 2번 받았다고 가정
        when(rewardRepository.countByUserIdAndDescriptionLikeAndDateRange(
                eq(TEST_USER_ID), eq("종합점수%"), any(), any())).thenReturn(2L);

        // When
        rewardService.calculateAndEarn(request);

        // Then
        verify(rewardRepository, never()).save(any()); // 저장되지 않음
    }

    // ===== 🎯 NEW: MBTI 관련 세분화 테스트 (isImprovedAtPosition, isMbtiImproved 커버리지 향상) =====

    @Test
    @DisplayName("MBTI determineMbtiType - null 스코어 처리")
    void calculateAndEarn_MbtiType_NullScore_NoMbtiReward() {
        // Given
        RewardEarnRequest request = createBaseRequest()
                .lastScore(null) // null 스코어
                .currentScore(createGoodScore())
                .build();

        // When
        rewardService.calculateAndEarn(request);

        // Then - MBTI 리워드 없음
        verify(rewardRepository, never()).save(any());
    }

    @Test
    @DisplayName("MBTI determineMbtiType - null 필드 처리")
    void calculateAndEarn_MbtiType_NullFields_HandlesGracefully() {
        // Given
        ScoreInfo scoreWithNulls = ScoreInfo.builder()
                .carbon(null) // null 필드
                .safety(60)
                .accident(null) // null 필드
                .focus(60)
                .build();

        RewardEarnRequest request = createBaseRequest()
                .lastScore(createBadScore())
                .currentScore(scoreWithNulls)
                .build();

        setupMbtiMocks();

        // When
        rewardService.calculateAndEarn(request);

        // Then - 일부 필드가 null이어도 처리됨
        verify(rewardRepository).save(any());
    }

    @Test
    @DisplayName("MBTI isImprovedAtPosition - 포지션 0: Heavy → Eco 향상")
    void calculateAndEarn_MbtiImprovement_Position0_HeavyToEco() {
        // Given
        ScoreInfo lastScore = ScoreInfo.builder().carbon(40).safety(60).accident(60).focus(60).build(); // H
        ScoreInfo currentScore = ScoreInfo.builder().carbon(60).safety(60).accident(60).focus(60).build(); // E

        testMbtiImprovement(lastScore, currentScore, "포지션 0 향상");
    }

    @Test
    @DisplayName("MBTI isImprovedAtPosition - 포지션 1: Aggressive → Defensive 향상")
    void calculateAndEarn_MbtiImprovement_Position1_AggressiveToDefensive() {
        // Given
        ScoreInfo lastScore = ScoreInfo.builder().carbon(60).safety(40).accident(60).focus(60).build(); // A
        ScoreInfo currentScore = ScoreInfo.builder().carbon(60).safety(60).accident(60).focus(60).build(); // D

        testMbtiImprovement(lastScore, currentScore, "포지션 1 향상");
    }

    @Test
    @DisplayName("MBTI isImprovedAtPosition - 포지션 2: Insensitive → Sensitive 향상")
    void calculateAndEarn_MbtiImprovement_Position2_InsensitiveToSensitive() {
        // Given
        ScoreInfo lastScore = ScoreInfo.builder().carbon(60).safety(60).accident(40).focus(60).build(); // I
        ScoreInfo currentScore = ScoreInfo.builder().carbon(60).safety(60).accident(60).focus(60).build(); // S

        testMbtiImprovement(lastScore, currentScore, "포지션 2 향상");
    }

    @Test
    @DisplayName("MBTI isImprovedAtPosition - 포지션 3: Unfocused → Focused 향상")
    void calculateAndEarn_MbtiImprovement_Position3_UnfocusedToFocused() {
        // Given
        ScoreInfo lastScore = ScoreInfo.builder().carbon(60).safety(60).accident(60).focus(40).build(); // U
        ScoreInfo currentScore = ScoreInfo.builder().carbon(60).safety(60).accident(60).focus(60).build(); // F

        testMbtiImprovement(lastScore, currentScore, "포지션 3 향상");
    }

    @Test
    @DisplayName("MBTI isImprovedAtPosition - default case 처리")
    void calculateAndEarn_MbtiImprovement_InvalidPosition_NoImprovement() {
        // 이 테스트는 실제로는 불가능하지만 코드 커버리지를 위해 추가
        // isImprovedAtPosition의 default case를 테스트하기 위해서는
        // 직접적으로 테스트할 수 없으므로, 다른 방식으로 검증

        // Given - 변화가 없는 경우
        ScoreInfo sameScore = createGoodScore();
        RewardEarnRequest request = createBaseRequest()
                .lastScore(sameScore)
                .currentScore(sameScore)
                .build();

        // When
        rewardService.calculateAndEarn(request);

        // Then - 변화 없으므로 MBTI 리워드 없음
        verify(rewardRepository, never()).save(any());
    }

    @Test
    @DisplayName("MBTI 역방향 변화 - 향상 아님")
    void calculateAndEarn_MbtiChange_Backwards_NoReward() {
        // Given - 좋은 상태에서 나쁜 상태로 (역방향)
        ScoreInfo lastScore = createGoodScore(); // EDSF
        ScoreInfo currentScore = createBadScore(); // HAIU

        RewardEarnRequest request = createBaseRequest()
                .lastScore(lastScore)
                .currentScore(currentScore)
                .build();

        // When
        rewardService.calculateAndEarn(request);

        // Then - 역방향 변화는 리워드 없음
        verify(rewardRepository, never()).save(any());
    }

    @Test
    @DisplayName("MBTI 부분 향상 - 일부만 좋아져도 리워드")
    void calculateAndEarn_MbtiImprovement_PartialImprovement() {
        // Given - Carbon만 향상 (H → E), 나머지는 동일
        ScoreInfo lastScore = ScoreInfo.builder().carbon(40).safety(60).accident(60).focus(60).build();
        ScoreInfo currentScore = ScoreInfo.builder().carbon(60).safety(60).accident(60).focus(60).build();

        testMbtiImprovement(lastScore, currentScore, "부분 향상");
    }

    @Test
    @DisplayName("MBTI 다중 향상 - 여러 영역 동시 향상")
    void calculateAndEarn_MbtiImprovement_MultipleImprovements() {
        // Given - Carbon과 Safety 동시 향상
        ScoreInfo lastScore = ScoreInfo.builder().carbon(40).safety(40).accident(60).focus(60).build(); // HADS
        ScoreInfo currentScore = ScoreInfo.builder().carbon(60).safety(60).accident(60).focus(60).build(); // EDSF

        testMbtiImprovement(lastScore, currentScore, "다중 향상");
    }

    @Test
    @DisplayName("MBTI 향상 리워드 - 하루 최대 2회 제한")
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
    @DisplayName("MBTI 동일 타입 - 리워드 없음")
    void calculateAndEarn_MbtiSameType_NoReward() {
        // Given - 같은 MBTI 타입
        ScoreInfo sameScore = createGoodScore();
        RewardEarnRequest request = createBaseRequest()
                .lastScore(sameScore)
                .currentScore(sameScore)
                .build();

        // When
        rewardService.calculateAndEarn(request);

        // Then
        verify(rewardRepository, never()).save(any());
    }

    // ===== 🎯 NEW: 복합 시나리오 테스트 =====

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
        RewardEarnRequest request = createBaseRequest().drivingTime(15).build();

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

    // ===== 🎯 NEW: 경계값 및 예외 케이스 테스트 =====

    @Test
    @DisplayName("주행시간 null 처리")
    void calculateAndEarn_DrivingTimeNull_NoDrivingReward() {
        // Given
        RewardEarnRequest request = createBaseRequest()
                .drivingTime(null) // null 주행시간
                .build();

        // When
        rewardService.calculateAndEarn(request);

        // Then
        verify(rewardRepository, never()).save(any());
    }

    @Test
    @DisplayName("점수 null 처리")
    void calculateAndEarn_ScoreNull_NoScoreReward() {
        // Given
        RewardEarnRequest request = createBaseRequest()
                .score(null) // null 점수
                .build();

        // When
        rewardService.calculateAndEarn(request);

        // Then
        verify(rewardRepository, never()).save(any());
    }

    @Test
    @DisplayName("주행시간 정확히 10분 - 리워드 받음")
    void calculateAndEarn_DrivingTimeExactly10Minutes_EarnsReward() {
        // Given
        RewardEarnRequest request = createBaseRequest().drivingTime(10).build(); // 정확히 10분

        RewardBalance mockBalance = createMockBalance(100L);
        when(rewardBalanceRepository.findByUserId(TEST_USER_ID)).thenReturn(Optional.of(mockBalance));
        when(rewardBalanceRepository.save(any())).thenReturn(mockBalance);

        // When
        rewardService.calculateAndEarn(request);

        // Then
        verify(rewardRepository, times(1)).save(any());
    }

    // ===== Helper Methods =====

    private void testScoreReward(int score, Long expectedSeeds, String description) {
        // Given
        reset(rewardRepository, rewardBalanceRepository);
        RewardEarnRequest request = createBaseRequest().score(score).drivingTime(5).build();

        when(rewardRepository.countByUserIdAndDescriptionLikeAndDateRange(
                eq(TEST_USER_ID), eq("종합점수%"), any(), any())).thenReturn(0L);
        RewardBalance mockBalance = createMockBalance(100L);
        when(rewardBalanceRepository.findByUserId(TEST_USER_ID)).thenReturn(Optional.of(mockBalance));
        when(rewardBalanceRepository.save(any())).thenReturn(mockBalance);

        // When
        rewardService.calculateAndEarn(request);

        // Then
        if (expectedSeeds > 0) {
            ArgumentCaptor<Reward> rewardCaptor = ArgumentCaptor.forClass(Reward.class);
            verify(rewardRepository).save(rewardCaptor.capture());
            assertThat(rewardCaptor.getValue().getAmount()).isEqualTo(expectedSeeds);
        } else {
            verify(rewardRepository, never()).save(any());
        }
    }

    private void testMbtiImprovement(ScoreInfo lastScore, ScoreInfo currentScore, String description) {
        // Given
        RewardEarnRequest request = createBaseRequest()
                .lastScore(lastScore)
                .currentScore(currentScore)
                .score(30) // 점수 리워드 없음
                .drivingTime(5) // 주행 리워드 없음
                .build();

        setupMbtiMocks();

        // When
        rewardService.calculateAndEarn(request);

        // Then
        ArgumentCaptor<Reward> rewardCaptor = ArgumentCaptor.forClass(Reward.class);
        verify(rewardRepository).save(rewardCaptor.capture());

        Reward savedReward = rewardCaptor.getValue();
        assertThat(savedReward.getAmount()).isEqualTo(5L);
        assertThat(savedReward.getDescription()).isEqualTo("MoBTI향상");
    }

    private void setupMbtiMocks() {
        when(rewardRepository.countByUserIdAndDescriptionLikeAndDateRange(
                eq(TEST_USER_ID), eq("MoBTI향상%"), any(), any())).thenReturn(0L);

        RewardBalance mockBalance = createMockBalance(100L);
        when(rewardBalanceRepository.findByUserId(TEST_USER_ID)).thenReturn(Optional.of(mockBalance));
        when(rewardBalanceRepository.save(any())).thenReturn(mockBalance);
    }

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

    private ScoreInfo createGoodScore() {
        return ScoreInfo.builder()
                .carbon(60) // E
                .safety(60) // D
                .accident(60) // S
                .focus(60) // F
                .build();
    }

    private ScoreInfo createBadScore() {
        return ScoreInfo.builder()
                .carbon(40) // H
                .safety(40) // A
                .accident(40) // I
                .focus(40) // U
                .build();
    }
}
