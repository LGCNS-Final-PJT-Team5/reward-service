package com.modive.rewardservice.service;

import com.modive.rewardservice.domain.Reward;
import com.modive.rewardservice.domain.RewardReason;
import com.modive.rewardservice.domain.RewardType;
import com.modive.rewardservice.dto.AdminRewardDto;
import com.modive.rewardservice.repository.RewardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminRewardServiceTest {

    @Mock
    private RewardRepository rewardRepository;

    @InjectMocks
    private AdminRewardService adminRewardService;

    private Reward sampleReward;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();
        sampleReward = Reward.builder()
                .userId("test-user-id")
                .amount(100L)
                .type(RewardType.EARNED)
                .description("종합점수")
                .balanceSnapshot(1000L)
                .driveId("drive-123")
                .build();

        // Reflection을 사용하여 createdAt 필드 설정 (테스트용)
        try {
            java.lang.reflect.Field createdAtField = Reward.class.getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(sampleReward, now);

            java.lang.reflect.Field idField = Reward.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(sampleReward, 1L);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set test fields", e);
        }
    }

    // ===== 기본 통계 조회 메서드 테스트 =====

    @Test
    @DisplayName("1. 총 발급 수 조회 성공")
    void getTotalIssued_Success() {
        // given
        given(rewardRepository.getTotalIssued()).willReturn(1247890L);

        // when
        long result = adminRewardService.getTotalIssued();

        // then
        assertThat(result).isEqualTo(1247890L);
        verify(rewardRepository).getTotalIssued();
    }

    @Test
    @DisplayName("2. 일일 변화율 조회 - 정상 케이스")
    void getChangeRate_Success() {
        // given
        given(rewardRepository.countIssuedBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .willReturn(150L) // 오늘
                .willReturn(100L); // 어제

        // when
        double result = adminRewardService.getChangeRate();

        // then
        assertThat(result).isEqualTo(50.0); // (150-100)/100 * 100 = 50%
        verify(rewardRepository, org.mockito.Mockito.times(2))
                .countIssuedBetween(any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("3. 일일 변화율 조회 - 어제 0건인 경우")
    void getChangeRate_YesterdayZero() {
        // given
        given(rewardRepository.countIssuedBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .willReturn(150L) // 오늘
                .willReturn(0L);  // 어제

        // when
        double result = adminRewardService.getChangeRate();

        // then
        assertThat(result).isEqualTo(100.0); // 어제 0건이면 100% 증가
    }

    @Test
    @DisplayName("4. 일일 변화율 조회 - 둘 다 0건인 경우")
    void getChangeRate_BothZero() {
        // given
        given(rewardRepository.countIssuedBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .willReturn(0L) // 오늘
                .willReturn(0L); // 어제

        // when
        double result = adminRewardService.getChangeRate();

        // then
        assertThat(result).isEqualTo(0.0); // 둘 다 0이면 0%
    }

    @Test
    @DisplayName("5. 현재 월 발급 수 조회 성공")
    void getCurrentMonthIssued_Success() {
        // given
        given(rewardRepository.countIssuedBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .willReturn(20700L);

        // when
        long result = adminRewardService.getCurrentMonthIssued();

        // then
        assertThat(result).isEqualTo(20700L);
    }

    @Test
    @DisplayName("6. 월간 변화율 조회 성공")
    void getMonthlyChangeRate_Success() {
        // given
        given(rewardRepository.countIssuedBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .willReturn(22500L) // 이번 달
                .willReturn(20000L); // 지난 달

        // when
        double result = adminRewardService.getMonthlyChangeRate();

        // then
        assertThat(result).isEqualTo(12.5); // (22500-20000)/20000 * 100 = 12.5%
    }

    @Test
    @DisplayName("7. 현재 일평균 발급 수 조회 성공")
    void getCurrentDailyAverageIssued_Success() {
        // given
        given(rewardRepository.countIssuedBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .willReturn(730L);

        // when
        double result = adminRewardService.getCurrentDailyAverageIssued();

        // then
        assertThat(result).isEqualTo(730.0);
    }

    @Test
    @DisplayName("8. 현재 사용자당 평균 발급 수 조회 성공")
    void getCurrentPerUserAverageIssued_Success() {
        // given
        given(rewardRepository.countIssuedBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .willReturn(1580L); // 총 리워드 수
        given(rewardRepository.countDistinctUsersBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .willReturn(10L); // 사용자 수

        // when
        double result = adminRewardService.getCurrentPerUserAverageIssued();

        // then
        assertThat(result).isEqualTo(158.0); // 1580/10 = 158.0
    }

    @Test
    @DisplayName("9. 사용자당 평균 발급 수 조회 - 사용자 0명인 경우")
    void getCurrentPerUserAverageIssued_NoUsers() {
        // given
        given(rewardRepository.countIssuedBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .willReturn(100L); // 총 리워드 수
        given(rewardRepository.countDistinctUsersBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .willReturn(0L); // 사용자 수 0명

        // when
        double result = adminRewardService.getCurrentPerUserAverageIssued();

        // then
        assertThat(result).isEqualTo(0.0);
    }

    @Test
    @DisplayName("10. 사용자당 평균 변화율 조회 성공")
    void getPerUserAverageChangeRate_Success() {
        // given
        // 오늘: 1600 리워드 / 10 사용자 = 160 평균
        given(rewardRepository.countIssuedBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .willReturn(1600L) // 오늘 리워드
                .willReturn(1500L); // 어제 리워드
        given(rewardRepository.countDistinctUsersBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .willReturn(10L) // 오늘 사용자
                .willReturn(10L); // 어제 사용자

        // when
        double result = adminRewardService.getPerUserAverageChangeRate();

        // then
        // 오늘 평균: 160, 어제 평균: 150, 변화율: (160-150)/150 * 100 = 6.67%
        assertThat(result).isEqualTo(6.7); // 반올림으로 6.7
    }

    // ===== 사유별 통계 조회 테스트 =====

    @Test
    @DisplayName("11. 총 사유별 통계 조회 성공")
    void getTotalRewardStats_Success() {
        // given
        Object[] stat1 = {"종합점수", 800L};
        Object[] stat2 = {"이벤트미발생", 400L};
        Object[] stat3 = {"MoBTI향상", 350L};
        List<Object[]> mockStats = Arrays.<Object[]>asList(stat1, stat2, stat3);
        given(rewardRepository.getCurrentYearIssuedGroupedByReason()).willReturn(mockStats);

        // when
        AdminRewardDto.TotalReasonStatsResponse result = adminRewardService.getTotalRewardStats();

        // then
        assertThat(result.getTotalRewardStatistics()).hasSize(3);
        assertThat(result.getTotalRewardStatistics().get(0).getReason()).isEqualTo("종합점수");
        assertThat(result.getTotalRewardStatistics().get(0).getCount()).isEqualTo(800L);
        assertThat(result.getTotalRewardStatistics().get(0).getRatio()).isEqualTo(51.6); // 800/1550 * 100
    }

    @Test
    @DisplayName("12. 총 사유별 통계 조회 - 예외 발생 시 빈 리스트 반환")
    void getTotalRewardStats_ExceptionHandling() {
        // given
        given(rewardRepository.getCurrentYearIssuedGroupedByReason())
                .willThrow(new RuntimeException("Database error"));

        // when
        AdminRewardDto.TotalReasonStatsResponse result = adminRewardService.getTotalRewardStats();

        // then
        assertThat(result.getTotalRewardStatistics()).isEmpty();
    }

    @Test
    @DisplayName("13. 월별 사유별 통계 조회 성공 - 현재 월")
    void getMonthlyRewardStatsByReason_CurrentMonth_Success() {
        // given
        Object[] stat1 = {"종합점수", 500L};
        Object[] stat2 = {"이벤트미발생", 300L};
        List<Object[]> mockStats = Arrays.<Object[]>asList(stat1, stat2);
        given(rewardRepository.getMonthlyRewardStatsByReason(anyString())).willReturn(mockStats);

        // when
        AdminRewardDto.MonthlyReasonStatsResponse result =
                adminRewardService.getMonthlyRewardStatsByReason(null);

        // then
        assertThat(result.getMonthlyRewardStatistics()).hasSize(2);
        assertThat(result.getMonthlyRewardStatistics().get(0).getReason()).isEqualTo("종합점수");
        assertThat(result.getMonthlyRewardStatistics().get(0).getCount()).isEqualTo(500L);
        assertThat(result.getMonthlyRewardStatistics().get(0).getRatio()).isEqualTo(62.5); // 500/800 * 100
    }

    @Test
    @DisplayName("14. 월별 사유별 통계 조회 성공 - 특정 월")
    void getMonthlyRewardStatsByReason_SpecificMonth_Success() {
        // given
        String targetMonth = "2025-04";
        Object[] stat1 = {"종합점수", 400L};
        List<Object[]> mockStats = Arrays.<Object[]>asList(stat1);
        given(rewardRepository.getMonthlyRewardStatsByReason(targetMonth)).willReturn(mockStats);

        // when
        AdminRewardDto.MonthlyReasonStatsResponse result =
                adminRewardService.getMonthlyRewardStatsByReason(targetMonth);

        // then
        assertThat(result.getMonthlyRewardStatistics()).hasSize(1);
        assertThat(result.getMonthlyRewardStatistics().get(0).getRatio()).isEqualTo(100.0);
        verify(rewardRepository).getMonthlyRewardStatsByReason(targetMonth);
    }

    // ===== 월별 트렌드 조회 테스트 =====

    @Test
    @DisplayName("15. 월별 리워드 트렌드 조회 성공")
    void getMonthlyRewardTrends_Success() {
        // given - 현재 시간 기준으로 동적으로 테스트 데이터 생성
        java.time.YearMonth currentMonth = java.time.YearMonth.now();
        java.time.YearMonth threeMonthsAgo = currentMonth.minusMonths(3);
        java.time.YearMonth sixMonthsAgo = currentMonth.minusMonths(6);
        java.time.YearMonth nineMonthsAgo = currentMonth.minusMonths(9);

        Object[] trend1 = {threeMonthsAgo.getYear(), threeMonthsAgo.getMonthValue(), 12500};
        Object[] trend2 = {sixMonthsAgo.getYear(), sixMonthsAgo.getMonthValue(), 13200};
        Object[] trend3 = {nineMonthsAgo.getYear(), nineMonthsAgo.getMonthValue(), 14100};
        List<Object[]> mockTrends = Arrays.<Object[]>asList(trend1, trend2, trend3);
        given(rewardRepository.findMonthlyIssuedStatsLast12Months(any(LocalDateTime.class)))
                .willReturn(mockTrends);

        // when
        AdminRewardDto.MonthlyStatsResponse result = adminRewardService.getMonthlyRewardTrends();

        // then
        assertThat(result.getMonthlyRewardStatistics()).hasSize(12); // 항상 12개월 데이터

        List<AdminRewardDto.MonthlyRewardStat> stats = result.getMonthlyRewardStatistics();

        // 실제 데이터가 있는 월들 확인
        boolean hasThreeMonthsAgo = stats.stream()
                .anyMatch(s -> s.getYear() == threeMonthsAgo.getYear()
                        && s.getMonth() == threeMonthsAgo.getMonthValue()
                        && s.getAmount() == 12500);
        assertThat(hasThreeMonthsAgo).isTrue();

        boolean hasSixMonthsAgo = stats.stream()
                .anyMatch(s -> s.getYear() == sixMonthsAgo.getYear()
                        && s.getMonth() == sixMonthsAgo.getMonthValue()
                        && s.getAmount() == 13200);
        assertThat(hasSixMonthsAgo).isTrue();

        boolean hasNineMonthsAgo = stats.stream()
                .anyMatch(s -> s.getYear() == nineMonthsAgo.getYear()
                        && s.getMonth() == nineMonthsAgo.getMonthValue()
                        && s.getAmount() == 14100);
        assertThat(hasNineMonthsAgo).isTrue();

        // 데이터가 없는 월은 amount가 0이어야 함
        long zeroAmountCount = stats.stream()
                .filter(s -> s.getAmount() == 0)
                .count();
        assertThat(zeroAmountCount).isEqualTo(9); // 12개월 중 3개월만 데이터가 있으므로 9개월은 0

        // 연속된 12개월이 생성되었는지 확인 (11개월 전부터 현재 월까지)
        java.time.YearMonth elevenMonthsAgo = currentMonth.minusMonths(11);
        boolean hasOldestMonth = stats.stream()
                .anyMatch(s -> s.getYear() == elevenMonthsAgo.getYear()
                        && s.getMonth() == elevenMonthsAgo.getMonthValue());
        assertThat(hasOldestMonth).isTrue();

        boolean hasCurrentMonth = stats.stream()
                .anyMatch(s -> s.getYear() == currentMonth.getYear()
                        && s.getMonth() == currentMonth.getMonthValue());
        assertThat(hasCurrentMonth).isTrue();
    }

    @Test
    @DisplayName("16. 월별 리워드 트렌드 조회 - 예외 발생 시 빈 리스트 반환")
    void getMonthlyRewardTrends_ExceptionHandling() {
        // given
        given(rewardRepository.findMonthlyIssuedStatsLast12Months(any(LocalDateTime.class)))
                .willThrow(new RuntimeException("Database error"));

        // when
        AdminRewardDto.MonthlyStatsResponse result = adminRewardService.getMonthlyRewardTrends();

        // then
        assertThat(result.getMonthlyRewardStatistics()).isEmpty();
    }

    // ===== 리워드 내역 조회 테스트 =====

    @Test
    @DisplayName("17. 전체 리워드 내역 조회 성공")
    void getAllRewardHistory_Success() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        List<Reward> rewards = Arrays.<Reward>asList(sampleReward);
        Page<Reward> rewardPage = new PageImpl<>(rewards, pageable, 1);

        given(rewardRepository.findAllByOrderByCreatedAtDesc(pageable)).willReturn(rewardPage);

        // when
        Page<AdminRewardDto.AllRewardHistoryResponse.RewardHistoryItem> result =
                adminRewardService.getAllRewardHistory(pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        AdminRewardDto.AllRewardHistoryResponse.RewardHistoryItem item = result.getContent().get(0);
        assertThat(item.getRewardId()).isEqualTo("SEED_1"); // ID 설정했으므로 정확히 확인 가능
        assertThat(item.getIssuedDate()).isEqualTo(now.toLocalDate());
        assertThat(item.getReason()).isEqualTo("종합점수");
        assertThat(item.getAmount()).isEqualTo(100);
    }

    @Test
    @DisplayName("18. 전체 리워드 내역 조회 - 예외 발생")
    void getAllRewardHistory_Exception() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        given(rewardRepository.findAllByOrderByCreatedAtDesc(pageable))
                .willThrow(new RuntimeException("Database error"));

        // when & then
        assertThatThrownBy(() -> adminRewardService.getAllRewardHistory(pageable))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("리워드 내역 조회에 실패했습니다.");
    }

    // ===== 필터링 조회 테스트 - 수정된 메서드 시그니처에 맞춤 =====

    @Test
    @DisplayName("19. 리워드 필터링 조회 성공 - userId 직접 사용")
    void filterRewards_Success() {
        // given
        String userId = "test-user-id";
        String email = "test@example.com";
        String description = "종합점수";
        LocalDate startDate = LocalDate.of(2025, 4, 1);
        LocalDate endDate = LocalDate.of(2025, 4, 30);
        Pageable pageable = PageRequest.of(0, 10);

        List<Reward> rewards = Arrays.<Reward>asList(sampleReward);
        Page<Reward> rewardPage = new PageImpl<>(rewards, pageable, 1);
        given(rewardRepository.filterRewards(
                eq(userId), eq(description), any(LocalDateTime.class), any(LocalDateTime.class), eq(pageable)))
                .willReturn(rewardPage);

        // when - 새로운 시그니처: userId를 첫 번째 파라미터로 전달
        AdminRewardDto.RewardFilterResponse result = adminRewardService.filterRewards(
                userId, email, description, startDate, endDate, pageable);

        // then
        assertThat(result.getSearchResult()).hasSize(1);
        AdminRewardDto.FilteredReward filteredReward = result.getSearchResult().get(0);
        assertThat(filteredReward.getRewardId()).isEqualTo("SEED_1");
        assertThat(filteredReward.getUserId()).isEqualTo(userId);
        assertThat(filteredReward.getDescription()).isEqualTo("종합점수");
        assertThat(filteredReward.getAmount()).isEqualTo(100);
        assertThat(filteredReward.getCreatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("20. 리워드 필터링 조회 - 잘못된 날짜 범위")
    void filterRewards_InvalidDateRange() {
        // given
        String userId = "test-user-id";
        LocalDate startDate = LocalDate.of(2025, 4, 30);
        LocalDate endDate = LocalDate.of(2025, 4, 1); // 시작일이 종료일보다 늦음
        Pageable pageable = PageRequest.of(0, 10);

        // when & then - 실제 Service는 try-catch로 IllegalArgumentException을 RuntimeException으로 감쌈
        assertThatThrownBy(() -> adminRewardService.filterRewards(
                userId, null, null, startDate, endDate, pageable))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("리워드 필터링에 실패했습니다.")
                .hasCause(new IllegalArgumentException("시작일은 종료일보다 이전이어야 합니다."));
    }

    @Test
    @DisplayName("21. 리워드 필터링 조회 - userId가 null인 경우")
    void filterRewards_NullUserId() {
        // given
        Pageable pageable = PageRequest.of(0, 10);

        List<Reward> rewards = Arrays.<Reward>asList(sampleReward);
        Page<Reward> rewardPage = new PageImpl<>(rewards, pageable, 1);
        given(rewardRepository.filterRewards(isNull(), isNull(), isNull(), isNull(), eq(pageable)))
                .willReturn(rewardPage);

        // when
        AdminRewardDto.RewardFilterResponse result = adminRewardService.filterRewards(
                null, null, null, null, null, pageable);

        // then
        assertThat(result.getSearchResult()).hasSize(1);
    }

    @Test
    @DisplayName("22. 리워드 필터링 조회 - 빈 결과")
    void filterRewards_EmptyResult() {
        // given
        String userId = "test-user-id";
        Pageable pageable = PageRequest.of(0, 10);

        Page<Reward> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        given(rewardRepository.filterRewards(eq(userId), isNull(), isNull(), isNull(), eq(pageable)))
                .willReturn(emptyPage);

        // when
        AdminRewardDto.RewardFilterResponse result = adminRewardService.filterRewards(
                userId, null, null, null, null, pageable);

        // then
        assertThat(result.getSearchResult()).isEmpty();
        assertThat(result.getPageInfo().getTotalElements()).isEqualTo(0);
    }

    // ===== 운전별 리워드 조회 테스트 =====

    @Test
    @DisplayName("23. 운전별 리워드 조회 성공")
    void getRewardsByDrive_Success() {
        // given
        List<String> driveIds = Arrays.<String>asList("drive1", "drive2", "drive3");
        AdminRewardDto.RewardsByDriveRequest request =
                new AdminRewardDto.RewardsByDriveRequest(driveIds);

        given(rewardRepository.sumAmountByDriveId("drive1")).willReturn(Optional.of(100));
        given(rewardRepository.sumAmountByDriveId("drive2")).willReturn(Optional.of(250));
        given(rewardRepository.sumAmountByDriveId("drive3")).willReturn(Optional.empty()); // 리워드 없음

        // when
        AdminRewardDto.RewardsByDriveResponse result = adminRewardService.getRewardsByDrive(request);

        // then
        assertThat(result.getRewardsByDrive()).hasSize(3);
        assertThat(result.getRewardsByDrive().get(0).getDriveId()).isEqualTo("drive1");
        assertThat(result.getRewardsByDrive().get(0).getRewards()).isEqualTo(100);
        assertThat(result.getRewardsByDrive().get(1).getRewards()).isEqualTo(250);
        assertThat(result.getRewardsByDrive().get(2).getRewards()).isEqualTo(0); // Optional.empty() → 0
    }

    @Test
    @DisplayName("24. 운전별 리워드 조회 - 빈 드라이브 ID 리스트")
    void getRewardsByDrive_EmptyDriveIds() {
        // given
        AdminRewardDto.RewardsByDriveRequest request =
                new AdminRewardDto.RewardsByDriveRequest(Collections.emptyList());

        // when
        AdminRewardDto.RewardsByDriveResponse result = adminRewardService.getRewardsByDrive(request);

        // then
        assertThat(result.getRewardsByDrive()).isEmpty();
    }

    @Test
    @DisplayName("25. 운전별 리워드 조회 - 예외 발생")
    void getRewardsByDrive_Exception() {
        // given
        List<String> driveIds = Arrays.<String>asList("drive1");
        AdminRewardDto.RewardsByDriveRequest request =
                new AdminRewardDto.RewardsByDriveRequest(driveIds);

        given(rewardRepository.sumAmountByDriveId("drive1"))
                .willThrow(new RuntimeException("Database error"));

        // when & then
        assertThatThrownBy(() -> adminRewardService.getRewardsByDrive(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("운전별 리워드 조회에 실패했습니다.");
    }

    // ===== 기타 헬퍼 메서드 테스트 =====

    @Test
    @DisplayName("26. 빈 리워드 리스트 매핑 테스트")
    void mapToFilteredRewards_EmptyList() {
        // given - 빈 페이지
        String userId = "test-user-id";
        Page<Reward> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);
        given(rewardRepository.filterRewards(eq(userId), isNull(), isNull(), isNull(), any(Pageable.class)))
                .willReturn(emptyPage);

        // when
        AdminRewardDto.RewardFilterResponse result = adminRewardService.filterRewards(
                userId, null, null, null, null, PageRequest.of(0, 10));

        // then
        assertThat(result.getSearchResult()).isEmpty();
        assertThat(result.getPageInfo().getTotalElements()).isEqualTo(0);
    }

    // ===== RewardReason enum 매핑 테스트 =====
    @Test
    @DisplayName("27. RewardReason enum 매핑 테스트")
    void rewardReasonMapping_Test() {
        // given & when & then
        assertThat(RewardReason.fromDescription("종합점수")).isEqualTo(RewardReason.TOTAL_SCORE);
        assertThat(RewardReason.fromDescription("이벤트미발생")).isEqualTo(RewardReason.EVENT_NOT_OCCURRED);
        assertThat(RewardReason.fromDescription("MoBTI향상")).isEqualTo(RewardReason.MOBTI_IMPROVEMENT);
        assertThat(RewardReason.fromDescription("알 수 없는 사유")).isEqualTo(RewardReason.UNKNOWN);

        assertThat(RewardReason.TOTAL_SCORE.getLabel()).isEqualTo("종합점수");
        assertThat(RewardReason.EVENT_NOT_OCCURRED.getLabel()).isEqualTo("이벤트미발생");
        assertThat(RewardReason.MOBTI_IMPROVEMENT.getLabel()).isEqualTo("MoBTI향상");
    }

    // ===== 추가 테스트 케이스 =====

    @Test
    @DisplayName("28. 필터링 조회 - 모든 파라미터 사용")
    void filterRewards_WithAllParameters() {
        // given
        String userId = "test-user-id";
        String email = "test@example.com";
        String description = "종합점수";
        LocalDate startDate = LocalDate.of(2025, 4, 1);
        LocalDate endDate = LocalDate.of(2025, 4, 30);
        Pageable pageable = PageRequest.of(0, 10);

        List<Reward> rewards = Arrays.<Reward>asList(sampleReward);
        Page<Reward> rewardPage = new PageImpl<>(rewards, pageable, 1);
        given(rewardRepository.filterRewards(
                eq(userId), eq(description), any(LocalDateTime.class), any(LocalDateTime.class), eq(pageable)))
                .willReturn(rewardPage);

        // when
        AdminRewardDto.RewardFilterResponse result = adminRewardService.filterRewards(
                userId, email, description, startDate, endDate, pageable);

        // then
        assertThat(result.getSearchResult()).hasSize(1);
        assertThat(result.getPageInfo().getTotalElements()).isEqualTo(1);

        AdminRewardDto.FilteredReward filteredReward = result.getSearchResult().get(0);
        assertThat(filteredReward.getRewardId()).isEqualTo("SEED_1");
        assertThat(filteredReward.getUserId()).isEqualTo(userId);
        assertThat(filteredReward.getDescription()).isEqualTo("종합점수");
        assertThat(filteredReward.getAmount()).isEqualTo(100);
        assertThat(filteredReward.getCreatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("29. 필터링 조회 - startDate만 있는 경우")
    void filterRewards_OnlyStartDate() {
        // given
        String userId = "test-user-id";
        LocalDate startDate = LocalDate.of(2025, 4, 1);
        Pageable pageable = PageRequest.of(0, 10);

        List<Reward> rewards = Arrays.<Reward>asList(sampleReward);
        Page<Reward> rewardPage = new PageImpl<>(rewards, pageable, 1);
        given(rewardRepository.filterRewards(
                eq(userId), isNull(), any(LocalDateTime.class), isNull(), eq(pageable)))
                .willReturn(rewardPage);

        // when
        AdminRewardDto.RewardFilterResponse result = adminRewardService.filterRewards(
                userId, null, null, startDate, null, pageable);

        // then
        assertThat(result.getSearchResult()).hasSize(1);
        verify(rewardRepository).filterRewards(
                eq(userId), isNull(), any(LocalDateTime.class), isNull(), eq(pageable));
    }

    @Test
    @DisplayName("30. 필터링 조회 - endDate만 있는 경우")
    void filterRewards_OnlyEndDate() {
        // given
        String userId = "test-user-id";
        LocalDate endDate = LocalDate.of(2025, 4, 30);
        Pageable pageable = PageRequest.of(0, 10);

        List<Reward> rewards = Arrays.<Reward>asList(sampleReward);
        Page<Reward> rewardPage = new PageImpl<>(rewards, pageable, 1);
        given(rewardRepository.filterRewards(
                eq(userId), isNull(), isNull(), any(LocalDateTime.class), eq(pageable)))
                .willReturn(rewardPage);

        // when
        AdminRewardDto.RewardFilterResponse result = adminRewardService.filterRewards(
                userId, null, null, null, endDate, pageable);

        // then
        assertThat(result.getSearchResult()).hasSize(1);
        verify(rewardRepository).filterRewards(
                eq(userId), isNull(), isNull(), any(LocalDateTime.class), eq(pageable));
    }

    @Test
    @DisplayName("31. 필터링 조회 - description만 있는 경우")
    void filterRewards_OnlyDescription() {
        // given
        String userId = "test-user-id";
        String description = "종합점수";
        Pageable pageable = PageRequest.of(0, 10);

        List<Reward> rewards = Arrays.<Reward>asList(sampleReward);
        Page<Reward> rewardPage = new PageImpl<>(rewards, pageable, 1);
        given(rewardRepository.filterRewards(
                eq(userId), eq(description), isNull(), isNull(), eq(pageable)))
                .willReturn(rewardPage);

        // when
        AdminRewardDto.RewardFilterResponse result = adminRewardService.filterRewards(
                userId, null, description, null, null, pageable);

        // then
        assertThat(result.getSearchResult()).hasSize(1);
        AdminRewardDto.FilteredReward filteredReward = result.getSearchResult().get(0);
        assertThat(filteredReward.getDescription()).isEqualTo("종합점수");
    }

    @Test
    @DisplayName("32. 필터링 조회 - Repository 예외 발생")
    void filterRewards_RepositoryException() {
        // given
        String userId = "test-user-id";
        Pageable pageable = PageRequest.of(0, 10);

        given(rewardRepository.filterRewards(
                eq(userId), isNull(), isNull(), isNull(), eq(pageable)))
                .willThrow(new RuntimeException("Database connection failed"));

        // when & then
        assertThatThrownBy(() -> adminRewardService.filterRewards(
                userId, null, null, null, null, pageable))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("리워드 필터링에 실패했습니다.")
                .hasCauseInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("33. 통계 조회 - 일평균 변화율 계산")
    void getDailyAverageChangeRate_Success() {
        // given
        given(rewardRepository.countIssuedBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .willReturn(160L) // 오늘
                .willReturn(150L); // 어제

        // when
        double result = adminRewardService.getDailyAverageChangeRate();

        // then
        assertThat(result).isEqualTo(6.7); // (160-150)/150 * 100 = 6.67% -> 반올림 6.7%
    }

    @Test
    @DisplayName("34. 필터링 조회 - 대량 데이터")
    void filterRewards_LargeDataSet() {
        // given
        String userId = "test-user-id";
        Pageable pageable = PageRequest.of(0, 100);

        // 대량 데이터 시뮬레이션
        List<Reward> largeRewardList = new ArrayList<>();
        for (int i = 1; i <= 50; i++) {
            Reward reward = Reward.builder()
                    .userId("user-" + i)
                    .amount((long) (i * 10))
                    .type(RewardType.EARNED)
                    .description("종합점수")
                    .balanceSnapshot(1000L + i)
                    .driveId("drive-" + i)
                    .build();

            // Reflection으로 ID와 createdAt 설정
            try {
                java.lang.reflect.Field idField = Reward.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(reward, (long) i);

                java.lang.reflect.Field createdAtField = Reward.class.getDeclaredField("createdAt");
                createdAtField.setAccessible(true);
                createdAtField.set(reward, now.plusMinutes(i));
            } catch (Exception e) {
                throw new RuntimeException("Failed to set test fields", e);
            }

            largeRewardList.add(reward);
        }

        Page<Reward> largePage = new PageImpl<>(largeRewardList, pageable, 50);
        given(rewardRepository.filterRewards(
                eq(userId), isNull(), isNull(), isNull(), eq(pageable)))
                .willReturn(largePage);

        // when
        AdminRewardDto.RewardFilterResponse result = adminRewardService.filterRewards(
                userId, null, null, null, null, pageable);

        // then
        assertThat(result.getSearchResult()).hasSize(50);
        assertThat(result.getPageInfo().getTotalElements()).isEqualTo(50);

        // 첫 번째와 마지막 요소 검증
        AdminRewardDto.FilteredReward firstReward = result.getSearchResult().get(0);
        assertThat(firstReward.getRewardId()).isEqualTo("SEED_1");
        assertThat(firstReward.getAmount()).isEqualTo(10);

        AdminRewardDto.FilteredReward lastReward = result.getSearchResult().get(49);
        assertThat(lastReward.getRewardId()).isEqualTo("SEED_50");
        assertThat(lastReward.getAmount()).isEqualTo(500);
    }
}