package com.modive.rewardservice.service;

import com.modive.rewardservice.client.UserClient;
import com.modive.rewardservice.domain.Reward;
import com.modive.rewardservice.domain.RewardReason;
import com.modive.rewardservice.domain.RewardType;
import com.modive.rewardservice.dto.AdminRewardDto;
import com.modive.rewardservice.repository.RewardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class AdminRewardServiceTest {

    @Autowired
    private AdminRewardService adminRewardService;

    @MockBean
    private RewardRepository rewardRepository;

    @MockBean
    private UserClient userClient;

    @BeforeEach
    void setUp() {
        // 🔧 간소화된 UserClient Mock 설정
        when(userClient.getUserIdByEmail(anyString())).thenReturn("1");
        when(userClient.getEmailByUserId(anyString())).thenReturn("user@example.com");
        // 🚫 제거: getEmailsByUserIds, existsByEmail, existsById
    }

    @Test
    @DisplayName("1. 총 발급 수 조회 - 전체 기간 총 발급 수와 변화율")
    void getTotalIssuedTest() {
        // given
        when(rewardRepository.getTotalIssued()).thenReturn(1247890L);

        LocalDateTime now = LocalDateTime.now();
        when(rewardRepository.countIssuedBefore(any())).thenReturn(1000000L, 800000L);

        // when
        long totalIssued = adminRewardService.getTotalIssued();
        double changeRate = adminRewardService.getChangeRate();

        // then
        assertThat(totalIssued).isEqualTo(1247890L);
        assertThat(changeRate).isEqualTo(25.0); // (1000000 - 800000) / 800000 * 100

        verify(rewardRepository, times(1)).getTotalIssued();
        verify(rewardRepository, times(2)).countIssuedBefore(any());
    }

    @Test
    @DisplayName("2. 월간 발급 수 조회 - 이번 달 발급 수와 지난 달 대비 변화율")
    void getMonthlyIssuedTest() {
        // given
        LocalDateTime currentStart = YearMonth.now().atDay(1).atStartOfDay();
        LocalDateTime currentEnd = YearMonth.now().atEndOfMonth().atTime(23, 59, 59);
        LocalDateTime prevStart = YearMonth.now().minusMonths(1).atDay(1).atStartOfDay();
        LocalDateTime prevEnd = YearMonth.now().minusMonths(1).atEndOfMonth().atTime(23, 59, 59);

        when(rewardRepository.countIssuedBetween(eq(currentStart), eq(currentEnd))).thenReturn(20700L);
        when(rewardRepository.countIssuedBetween(eq(prevStart), eq(prevEnd))).thenReturn(18400L);

        // when
        long monthlyIssued = adminRewardService.getCurrentMonthIssued();
        double changeRate = adminRewardService.getMonthlyChangeRate();

        // then
        assertThat(monthlyIssued).isEqualTo(20700L);
        assertThat(changeRate).isEqualTo(12.5); // (20700 - 18400) / 18400 * 100
    }

    @Test
    @DisplayName("3. 일 평균 발급 수 조회 - 오늘 발급 수와 어제 대비 변화율")
    void getDailyAverageChangeRateTest() {
        // given
        when(rewardRepository.countIssuedBetween(any(), any()))
                .thenReturn(730L)  // todayCount
                .thenReturn(690L); // yesterdayCount

        // when
        double result = adminRewardService.getDailyAverageChangeRate();

        // then
        assertThat(result).isEqualTo(5.8); // (730 - 690) / 690 * 100 = 5.8
    }

    @Test
    @DisplayName("4. 사용자당 평균 발급 수 조회 - 오늘 사용자당 평균과 어제 대비 변화율")
    void getPerUserAverageChangeRateTest() {
        // given
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.atTime(23, 59, 59);

        LocalDateTime yesterdayStart = today.minusDays(1).atStartOfDay();
        LocalDateTime yesterdayEnd = today.minusDays(1).atTime(23, 59, 59);

        when(rewardRepository.countIssuedBetween(todayStart, todayEnd)).thenReturn(15800L);
        when(rewardRepository.countDistinctUsersBetween(todayStart, todayEnd)).thenReturn(100L);

        when(rewardRepository.countIssuedBetween(yesterdayStart, yesterdayEnd)).thenReturn(15480L);
        when(rewardRepository.countDistinctUsersBetween(yesterdayStart, yesterdayEnd)).thenReturn(100L);

        // when
        double result = adminRewardService.getPerUserAverageChangeRate();

        // then
        assertThat(result).isEqualTo(2.1); // (158 - 154.8) / 154.8 * 100
    }

    @Test
    @DisplayName("5. 발급 사유별 총 통계 조회 - 실제 RewardReason 기준")
    void getTotalRewardStatsTest() {
        // given - 실제 RewardReason enum 값들 사용
        List<Object[]> rawStats = List.of(
                new Object[]{"종합점수", 1200L},          // TOTAL_SCORE
                new Object[]{"이벤트미발생", 3400L},       // EVENT_NOT_OCCURRED
                new Object[]{"MoBTI향상", 670L}          // MOBTI_IMPROVEMENT
        );
        when(rewardRepository.getCurrentYearIssuedGroupedByReason()).thenReturn(rawStats);

        // when
        AdminRewardDto.TotalReasonStatsResponse result = adminRewardService.getTotalRewardStats();

        // then
        assertThat(result.getTotalRewardStatistics()).hasSize(3);

        AdminRewardDto.ReasonStat firstStat = result.getTotalRewardStatistics().get(0);
        assertThat(firstStat.getReason()).isEqualTo("종합점수");
        assertThat(firstStat.getCount()).isEqualTo(1200L);
        assertThat(firstStat.getRatio()).isGreaterThan(0);

        // 각 사유별 통계 검증
        Map<String, Long> reasonCounts = new HashMap<>();
        result.getTotalRewardStatistics().forEach(stat ->
                reasonCounts.put(stat.getReason(), stat.getCount()));

        assertThat(reasonCounts.get("종합점수")).isEqualTo(1200L);
        assertThat(reasonCounts.get("이벤트미발생")).isEqualTo(3400L);
        assertThat(reasonCounts.get("MoBTI향상")).isEqualTo(670L);
    }

    @Test
    @DisplayName("5-2. 발급 사유별 월별 통계 조회 - 현재 월 (파라미터 없음)")
    void getMonthlyRewardStatsByReasonCurrentMonthTest() {
        // given - 실제 RewardReason 기준
        List<Object[]> rawStats = List.of(
                new Object[]{"종합점수", 800L},
                new Object[]{"이벤트미발생", 2400L},
                new Object[]{"MoBTI향상", 300L}
        );
        when(rewardRepository.getMonthlyRewardStatsByReason(anyString())).thenReturn(rawStats);

        // when - null 파라미터로 현재 월 조회
        AdminRewardDto.MonthlyReasonStatsResponse result =
                adminRewardService.getMonthlyRewardStatsByReason(null);

        // then
        assertThat(result.getMonthlyRewardStatistics()).hasSize(3);

        AdminRewardDto.ReasonStat firstStat = result.getMonthlyRewardStatistics().get(0);
        assertThat(firstStat.getReason()).isEqualTo("종합점수");
        assertThat(firstStat.getCount()).isEqualTo(800L);

        // Repository 메서드가 현재 월 형식으로 호출되었는지 확인
        verify(rewardRepository, times(1)).getMonthlyRewardStatsByReason(anyString());
    }

    @Test
    @DisplayName("5-3. 발급 사유별 월별 통계 조회 - 특정 월 파라미터")
    void getMonthlyRewardStatsByReasonWithParamTest() {
        // given
        String targetMonth = "2025-04";
        List<Object[]> rawStats = List.of(
                new Object[]{"종합점수", 650L},
                new Object[]{"이벤트미발생", 1800L},
                new Object[]{"MoBTI향상", 250L}
        );
        when(rewardRepository.getMonthlyRewardStatsByReason(targetMonth)).thenReturn(rawStats);

        // when
        AdminRewardDto.MonthlyReasonStatsResponse result =
                adminRewardService.getMonthlyRewardStatsByReason(targetMonth);

        // then
        assertThat(result.getMonthlyRewardStatistics()).hasSize(3);

        Map<String, Long> reasonCounts = new HashMap<>();
        result.getMonthlyRewardStatistics().forEach(stat ->
                reasonCounts.put(stat.getReason(), stat.getCount()));

        assertThat(reasonCounts.get("종합점수")).isEqualTo(650L);
        assertThat(reasonCounts.get("이벤트미발생")).isEqualTo(1800L);
        assertThat(reasonCounts.get("MoBTI향상")).isEqualTo(250L);

        verify(rewardRepository, times(1)).getMonthlyRewardStatsByReason(targetMonth);
    }

    @Test
    @DisplayName("6. 월별 씨앗 지급 통계 조회 - 최근 12개월 데이터")
    void getMonthlyRewardTrendsTest() {
        // given
        List<Object[]> rawStats = List.of(
                new Object[]{2024, 4, 12500},
                new Object[]{2024, 5, 13000},
                new Object[]{2024, 6, 12800}
        );
        when(rewardRepository.findMonthlyIssuedStatsLast12Months(any())).thenReturn(rawStats);

        // when
        AdminRewardDto.MonthlyStatsResponse result = adminRewardService.getMonthlyRewardTrends();

        // then
        assertThat(result.getMonthlyRewardStatistics()).hasSize(12); // 항상 12개 있어야 함

        // 특정 월에 대한 값만 검증
        AdminRewardDto.MonthlyRewardStat juneStat = result.getMonthlyRewardStatistics().stream()
                .filter(stat -> stat.getYear() == 2024 && stat.getMonth() == 6)
                .findFirst()
                .orElseThrow(() -> new AssertionError("2024년 6월 통계 없음"));

        assertThat(juneStat.getAmount()).isEqualTo(12800);
    }

    @Test
    @DisplayName("7. 최근 씨앗 발급 내역 조회 - 페이징 처리 및 RewardReason 변환")
    void getAllRewardHistoryTest() {
        // given
        Reward reward = Reward.builder()
                .userId(1L)
                .amount(12L)
                .type(RewardType.EARNED)
                .description("종합점수")
                .balanceSnapshot(1000L)
                .build();

        // Reflection으로 id와 createdAt 설정
        ReflectionTestUtils.setField(reward, "id", 1024L);
        ReflectionTestUtils.setField(reward, "createdAt", LocalDateTime.of(2025, 4, 25, 10, 30));

        Pageable pageable = PageRequest.of(0, 10);
        Page<Reward> page = new PageImpl<>(List.of(reward));
        when(rewardRepository.findAllByOrderByCreatedAtDesc(pageable)).thenReturn(page);

        // when
        Page<AdminRewardDto.AllRewardHistoryResponse.RewardHistoryItem> result =
                adminRewardService.getAllRewardHistory(pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getRewardId()).isEqualTo("SEED_1024");
        assertThat(result.getContent().get(0).getIssuedDate()).isEqualTo(LocalDate.of(2025, 4, 25));
        assertThat(result.getContent().get(0).getReason()).isEqualTo("종합점수");
        assertThat(result.getContent().get(0).getAmount()).isEqualTo(12);
    }

    @Test
    @DisplayName("8. 씨앗 필터링 조회 - userId 기반 간소화된 처리")
    void filterRewardsTest() {
        // given
        Reward reward = Reward.builder()
                .userId(1L)
                .amount(5L)
                .type(RewardType.EARNED)
                .description("종합점수")
                .balanceSnapshot(1000L)
                .build();

        // Reflection으로 id와 createdAt 설정
        ReflectionTestUtils.setField(reward, "id", 1025L);
        ReflectionTestUtils.setField(reward, "createdAt", LocalDateTime.of(2025, 4, 26, 12, 43, 45));

        Pageable pageable = PageRequest.of(0, 10);
        Page<Reward> page = new PageImpl<>(List.of(reward), pageable, 40);

        when(rewardRepository.filterRewards(any(), any(), any(), any(), any())).thenReturn(page);

        // when
        AdminRewardDto.RewardFilterResponse result = adminRewardService.filterRewards(
                "user1@example.com",
                "종합점수",
                LocalDate.of(2025, 4, 1),
                LocalDate.of(2025, 4, 30),
                pageable
        );

        // then
        assertThat(result.getSearchResult()).hasSize(1);
        assertThat(result.getSearchResult().get(0).getRewardId()).isEqualTo("SEED_1025");
        assertThat(result.getSearchResult().get(0).getUserId()).isEqualTo("1");  // 🔧 email → userId
        assertThat(result.getSearchResult().get(0).getAmount()).isEqualTo(5);
        assertThat(result.getSearchResult().get(0).getDescription()).isEqualTo("종합점수");
        assertThat(result.getPageInfo().getTotalElements()).isEqualTo(40);

        // 🔧 간소화된 UserClient 호출 확인
        verify(userClient, times(1)).getUserIdByEmail("user1@example.com");
        // 🚫 배치 조회는 더 이상 사용하지 않음
    }

    @Test
    @DisplayName("8-2. 씨앗 고급 검색 조회 - 새로운 검색 메서드")
    void searchRewardsTest() {
        // given
        AdminRewardDto.RewardSearchRequest searchRequest = AdminRewardDto.RewardSearchRequest.builder()
                .email("user1@example.com")
                .description("종합점수")
                .startDate(LocalDate.of(2025, 4, 1))
                .endDate(LocalDate.of(2025, 4, 30))
                .minAmount(1L)
                .maxAmount(100L)
                .build();

        Reward reward = Reward.builder()
                .userId(1L)
                .amount(5L)
                .type(RewardType.EARNED)
                .description("종합점수")
                .balanceSnapshot(1000L)
                .build();

        ReflectionTestUtils.setField(reward, "id", 1025L);
        ReflectionTestUtils.setField(reward, "createdAt", LocalDateTime.of(2025, 4, 26, 12, 43, 45));

        Pageable pageable = PageRequest.of(0, 10);
        Page<Reward> page = new PageImpl<>(List.of(reward), pageable, 40);

        when(rewardRepository.searchRewards(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(page);

        // when
        AdminRewardDto.RewardFilterResponse result = adminRewardService.searchRewards(
                searchRequest,
                pageable
        );

        // then
        assertThat(result.getSearchResult()).hasSize(1);
        assertThat(result.getSearchResult().get(0).getRewardId()).isEqualTo("SEED_1025");
        assertThat(result.getSearchResult().get(0).getUserId()).isEqualTo("1");
        assertThat(result.getSearchResult().get(0).getAmount()).isEqualTo(5);

        verify(userClient, times(1)).getUserIdByEmail("user1@example.com");
        verify(rewardRepository, times(1)).searchRewards(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("9. 운전별 씨앗 적립 내역 조회")
    void getRewardsByDriveTest() {
        // given
        List<String> driveIds = List.of("1", "2", "3", "4");
        AdminRewardDto.RewardsByDriveRequest request = new AdminRewardDto.RewardsByDriveRequest(driveIds);

        when(rewardRepository.sumAmountByDriveId("1")).thenReturn(Optional.of(100));
        when(rewardRepository.sumAmountByDriveId("2")).thenReturn(Optional.empty());
        when(rewardRepository.sumAmountByDriveId("3")).thenReturn(Optional.of(90));
        when(rewardRepository.sumAmountByDriveId("4")).thenReturn(Optional.of(80));

        // when
        AdminRewardDto.RewardsByDriveResponse result = adminRewardService.getRewardsByDrive(request);

        // then
        assertThat(result.getRewardsByDrive()).hasSize(4);
        assertThat(result.getRewardsByDrive().get(0).getDriveId()).isEqualTo("1");
        assertThat(result.getRewardsByDrive().get(0).getRewards()).isEqualTo(100);
        assertThat(result.getRewardsByDrive().get(1).getDriveId()).isEqualTo("2");
        assertThat(result.getRewardsByDrive().get(1).getRewards()).isEqualTo(0);
        assertThat(result.getRewardsByDrive().get(2).getDriveId()).isEqualTo("3");
        assertThat(result.getRewardsByDrive().get(2).getRewards()).isEqualTo(90);

        verify(rewardRepository, times(4)).sumAmountByDriveId(any());
    }

    @Test
    @DisplayName("10. 날짜 범위 검증 - 시작일이 종료일보다 늦을 때 예외 발생")
    void filterRewardsWithInvalidDateRangeTest() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        LocalDate startDate = LocalDate.of(2025, 4, 30);
        LocalDate endDate = LocalDate.of(2025, 4, 1);

        // when & then
        assertThatThrownBy(() -> adminRewardService.filterRewards(
                "user@example.com",
                null,
                startDate,
                endDate,
                pageable
        )).isInstanceOf(RuntimeException.class)
                .hasMessage("리워드 필터링에 실패했습니다.")
                .hasCauseInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("11. 사용자 존재하지 않을 때 빈 결과 반환")
    void filterRewardsWithNonExistentUserTest() {
        // given
        when(userClient.getUserIdByEmail("nonexistent@example.com")).thenReturn(null);
        Pageable pageable = PageRequest.of(0, 10);

        // when
        AdminRewardDto.RewardFilterResponse result = adminRewardService.filterRewards(
                "nonexistent@example.com",
                null,
                null,
                null,
                pageable
        );

        // then
        assertThat(result.getSearchResult()).isEmpty();
        assertThat(result.getPageInfo().getTotalElements()).isEqualTo(0);

        verify(userClient, times(1)).getUserIdByEmail("nonexistent@example.com");
        verify(rewardRepository, never()).filterRewards(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("12. 변화율 계산 - 이전 값이 0일 때 처리")
    void testChangeRateWhenPreviousValueIsZero() {
        // given
        when(rewardRepository.countIssuedBefore(any()))
                .thenReturn(100L)  // 한 달 전
                .thenReturn(0L);   // 두 달 전

        // when
        double changeRate = adminRewardService.getChangeRate();

        // then
        assertThat(changeRate).isEqualTo(100.0); // 이전 값이 0일 때는 100% 증가
    }

    @Test
    @DisplayName("13. 일일 평균 - 사용자가 없을 때 처리")
    void testPerUserAverageWhenNoUsers() {
        // given
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = LocalDate.now().atTime(23, 59, 59);

        when(rewardRepository.countIssuedBetween(start, end)).thenReturn(0L);
        when(rewardRepository.countDistinctUsersBetween(start, end)).thenReturn(0L);

        // when
        double perUserAverage = adminRewardService.getCurrentPerUserAverageIssued();

        // then
        assertThat(perUserAverage).isEqualTo(0.0);
    }

    @Test
    @DisplayName("14. 검색 요청 검증 실패 테스트")
    void testSearchRewardsValidationFailure() {
        // given - 잘못된 검색 요청 (시작일 > 종료일)
        AdminRewardDto.RewardSearchRequest invalidRequest = AdminRewardDto.RewardSearchRequest.builder()
                .email("user1@example.com")
                .startDate(LocalDate.of(2025, 4, 30))
                .endDate(LocalDate.of(2025, 4, 1))  // 시작일보다 이른 종료일
                .build();

        Pageable pageable = PageRequest.of(0, 10);

        // when & then
        assertThatThrownBy(() -> adminRewardService.searchRewards(invalidRequest, pageable))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("리워드 검색에 실패했습니다.")
                .hasCauseInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("15. 월별 통계 - 데이터가 없는 월 처리")
    void testMonthlyStatsWithMissingMonths() {
        // given
        List<Object[]> rawStats = List.of(
                new Object[]{2024, 6, 12500},
                new Object[]{2024, 8, 13000}  // 7월 데이터 없음
        );
        when(rewardRepository.findMonthlyIssuedStatsLast12Months(any())).thenReturn(rawStats);

        // when
        AdminRewardDto.MonthlyStatsResponse result = adminRewardService.getMonthlyRewardTrends();

        // then
        assertThat(result.getMonthlyRewardStatistics()).hasSize(12);

        // 데이터가 있는 월 확인
        AdminRewardDto.MonthlyRewardStat juneStat = result.getMonthlyRewardStatistics().stream()
                .filter(stat -> stat.getMonth() == 6 && stat.getYear() == 2024)
                .findFirst()
                .orElse(null);
        assertThat(juneStat).isNotNull();
        assertThat(juneStat.getAmount()).isEqualTo(12500);

        // 데이터가 없는 월은 0으로 처리
        AdminRewardDto.MonthlyRewardStat julyStat = result.getMonthlyRewardStatistics().stream()
                .filter(stat -> stat.getMonth() == 7 && stat.getYear() == 2024)
                .findFirst()
                .orElse(null);
        assertThat(julyStat).isNotNull();
        assertThat(julyStat.getAmount()).isEqualTo(0);
    }

    @Test
    @DisplayName("16. 운전별 적립 - 빈 driveIds 처리")
    void testRewardsByDriveWithEmptyList() {
        // given
        AdminRewardDto.RewardsByDriveRequest request = new AdminRewardDto.RewardsByDriveRequest(List.of());

        // when
        AdminRewardDto.RewardsByDriveResponse result = adminRewardService.getRewardsByDrive(request);

        // then
        assertThat(result.getRewardsByDrive()).isEmpty();
        verify(rewardRepository, never()).sumAmountByDriveId(any());
    }

    @Test
    @DisplayName("17. 월별 사유별 통계 - 잘못된 월 형식 처리")
    void testMonthlyReasonStatsWithInvalidMonthFormat() {
        // given
        String invalidMonth = "invalid-format";
        when(rewardRepository.getMonthlyRewardStatsByReason(invalidMonth))
                .thenReturn(List.of()); // 빈 결과 반환

        // when
        AdminRewardDto.MonthlyReasonStatsResponse result =
                adminRewardService.getMonthlyRewardStatsByReason(invalidMonth);

        // then
        assertThat(result.getMonthlyRewardStatistics()).isEmpty();
        verify(rewardRepository, times(1)).getMonthlyRewardStatsByReason(invalidMonth);
    }

    @Test
    @DisplayName("18. 성능 테스트 - 대용량 데이터 처리")
    void testLargeDataProcessing() {
        // given - 대용량 데이터 시뮬레이션
        when(rewardRepository.getTotalIssued()).thenReturn(50_000_000L); // 5천만개
        when(rewardRepository.countIssuedBefore(any()))
                .thenReturn(45_000_000L)    // 한달 전
                .thenReturn(40_000_000L);   // 두달 전

        // when
        long totalIssued = adminRewardService.getTotalIssued();
        double changeRate = adminRewardService.getChangeRate();

        // then
        assertThat(totalIssued).isEqualTo(50_000_000L);
        assertThat(changeRate).isEqualTo(12.5); // (45M - 40M) / 40M * 100

        // 성능 검증 - 타임아웃 없이 완료되어야 함
        verify(rewardRepository, times(1)).getTotalIssued();
    }

    @Test
    @DisplayName("19. 엣지 케이스 - 음수 리워드 처리")
    void testNegativeRewardHandling() {
        // given - 차감된 리워드가 있는 경우
        Reward negativeReward = Reward.builder()
                .userId(1L)
                .amount(-10L)  // 음수 리워드 (차감)
                .type(RewardType.USED)
                .description("종합점수")
                .balanceSnapshot(990L)
                .build();

        ReflectionTestUtils.setField(negativeReward, "id", 2000L);
        ReflectionTestUtils.setField(negativeReward, "createdAt", LocalDateTime.now());

        Pageable pageable = PageRequest.of(0, 10);
        Page<Reward> page = new PageImpl<>(List.of(negativeReward));
        when(rewardRepository.findAllByOrderByCreatedAtDesc(pageable)).thenReturn(page);

        // when
        Page<AdminRewardDto.AllRewardHistoryResponse.RewardHistoryItem> result =
                adminRewardService.getAllRewardHistory(pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getAmount()).isEqualTo(-10); // 음수 값 그대로 표시
        assertThat(result.getContent().get(0).getRewardId()).isEqualTo("SEED_2000");
    }

    @Test
    @DisplayName("20. 발급 사유별 통계 - 알 수 없는 사유 처리")
    void testReasonStatsWithUnknownReason() {
        // given
        List<Object[]> rawStats = List.of(
                new Object[]{"종합점수", 1200L},
                new Object[]{"이벤트미발생", 3400L},
                new Object[]{"알 수 없는 사유", 500L}  // RewardReason에 없는 사유
        );
        when(rewardRepository.getCurrentYearIssuedGroupedByReason()).thenReturn(rawStats);

        // when
        AdminRewardDto.TotalReasonStatsResponse result = adminRewardService.getTotalRewardStats();

        // then
        assertThat(result.getTotalRewardStatistics()).hasSize(3);

        // 알 수 없는 사유는 "알 수 없음"으로 변환됨 (RewardReason.UNKNOWN)
        AdminRewardDto.ReasonStat unknownStat = result.getTotalRewardStatistics().stream()
                .filter(stat -> stat.getReason().equals("알 수 없음"))
                .findFirst()
                .orElse(null);
        assertThat(unknownStat).isNotNull();
        assertThat(unknownStat.getCount()).isEqualTo(500L);
    }

    @Test
    @DisplayName("21. 필터링 - 모든 파라미터가 null일 때 처리")
    void testFilterWithNullParameters() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Reward> emptyPage = Page.empty(pageable);

        when(rewardRepository.filterRewards(isNull(), isNull(), isNull(), isNull(), eq(pageable)))
                .thenReturn(emptyPage);

        // when
        AdminRewardDto.RewardFilterResponse result = adminRewardService.filterRewards(
                null, null, null, null, pageable
        );

        // then
        assertThat(result.getSearchResult()).isEmpty();
        assertThat(result.getPageInfo().getTotalElements()).isEqualTo(0);

        verify(userClient, never()).getUserIdByEmail(anyString());
    }

    @Test
    @DisplayName("22. 비즈니스 로직 테스트 - 이벤트미발생 리워드 검증")
    void testEventNotOccurredRewardLogic() {
        // given - 이벤트미발생이 가장 많은 경우 (안전운전 우수)
        List<Object[]> rawStats = List.of(
                new Object[]{"이벤트미발생", 5000L},       // 가장 많음 (안전운전)
                new Object[]{"종합점수", 2000L},
                new Object[]{"MoBTI향상", 1000L}
        );
        when(rewardRepository.getCurrentYearIssuedGroupedByReason()).thenReturn(rawStats);

        // when
        AdminRewardDto.TotalReasonStatsResponse result = adminRewardService.getTotalRewardStats();

        // then
        AdminRewardDto.ReasonStat eventNotOccurredStat = result.getTotalRewardStatistics().stream()
                .filter(stat -> stat.getReason().equals("이벤트미발생"))
                .findFirst()
                .orElseThrow();

        assertThat(eventNotOccurredStat.getCount()).isEqualTo(5000L);
        assertThat(eventNotOccurredStat.getRatio()).isGreaterThan(50.0); // 과반수 이상
    }

    @Test
    @DisplayName("23. 비즈니스 로직 테스트 - MoBTI향상 리워드 검증")
    void testMobtiImprovementRewardLogic() {
        // given - MoBTI향상 리워드 케이스
        List<Object[]> rawStats = List.of(
                new Object[]{"종합점수", 3000L},
                new Object[]{"MoBTI향상", 1500L},          // 운전 성향 개선
                new Object[]{"이벤트미발생", 2500L}
        );
        when(rewardRepository.getCurrentYearIssuedGroupedByReason()).thenReturn(rawStats);

        // when
        AdminRewardDto.TotalReasonStatsResponse result = adminRewardService.getTotalRewardStats();

        // then
        AdminRewardDto.ReasonStat mobtiStat = result.getTotalRewardStatistics().stream()
                .filter(stat -> stat.getReason().equals("MoBTI향상"))
                .findFirst()
                .orElseThrow();

        assertThat(mobtiStat.getCount()).isEqualTo(1500L);
        assertThat(mobtiStat.getRatio()).isGreaterThan(0); // 향상된 사용자들이 있음
    }

    @Test
    @DisplayName("24. 월별 사유별 통계 - 계절별 패턴 테스트")
    void testSeasonalRewardPatterns() {
        // given - 겨울철 안전운전이 더 중요한 시기 (가정)
        String winterMonth = "2024-12";
        List<Object[]> winterStats = List.of(
                new Object[]{"이벤트미발생", 4000L},       // 겨울철 안전운전 증가
                new Object[]{"종합점수", 2000L},
                new Object[]{"MoBTI향상", 500L}
        );
        when(rewardRepository.getMonthlyRewardStatsByReason(winterMonth)).thenReturn(winterStats);

        // when
        AdminRewardDto.MonthlyReasonStatsResponse result =
                adminRewardService.getMonthlyRewardStatsByReason(winterMonth);

        // then
        AdminRewardDto.ReasonStat safetyReward = result.getMonthlyRewardStatistics().stream()
                .filter(stat -> stat.getReason().equals("이벤트미발생"))
                .findFirst()
                .orElseThrow();

        assertThat(safetyReward.getCount()).isEqualTo(4000L);
        assertThat(safetyReward.getRatio()).isGreaterThan(60.0); // 겨울철 안전운전 비중 높음
    }

    @Test
    @DisplayName("25. 모든 리워드 사유 테스트 - RewardReason enum 전체")
    void testAllRewardReasonValues() {
        // given - 모든 RewardReason 값들
        List<Object[]> rawStats = List.of(
                new Object[]{"종합점수", 1200L},          // TOTAL_SCORE
                new Object[]{"이벤트미발생", 3400L},       // EVENT_NOT_OCCURRED
                new Object[]{"MoBTI향상", 670L},          // MOBTI_IMPROVEMENT
                new Object[]{"알 수 없음", 100L}          // UNKNOWN
        );
        when(rewardRepository.getCurrentYearIssuedGroupedByReason()).thenReturn(rawStats);

        // when
        AdminRewardDto.TotalReasonStatsResponse result = adminRewardService.getTotalRewardStats();

        // then
        assertThat(result.getTotalRewardStatistics()).hasSize(4);

        // 각 사유별로 정확히 매핑되는지 확인
        Map<String, Long> reasonMap = result.getTotalRewardStatistics().stream()
                .collect(java.util.stream.Collectors.toMap(
                        AdminRewardDto.ReasonStat::getReason,
                        AdminRewardDto.ReasonStat::getCount
                ));

        assertThat(reasonMap.get("종합점수")).isEqualTo(1200L);
        assertThat(reasonMap.get("이벤트미발생")).isEqualTo(3400L);
        assertThat(reasonMap.get("MoBTI향상")).isEqualTo(670L);
        assertThat(reasonMap.get("알 수 없음")).isEqualTo(100L);

        // 비율 합계가 100%인지 확인
        double totalRatio = result.getTotalRewardStatistics().stream()
                .mapToDouble(AdminRewardDto.ReasonStat::getRatio)
                .sum();
        assertThat(totalRatio).isCloseTo(100.0, org.assertj.core.data.Offset.offset(0.1));
    }
}