package com.modive.rewardservice.service;

import com.modive.rewardservice.client.UserClient;
import com.modive.rewardservice.domain.Reward;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
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
        // 기본 mock 설정
        when(userClient.getUserIdByEmail(anyString())).thenReturn(1L);
        when(userClient.getEmailByUserId(anyLong())).thenReturn("user@example.com");
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
    @DisplayName("5. 발급 사유별 총 통계 조회 - 올해 데이터 3가지 카테고리 분류")
    void getTotalIssuedByReasonTest() {
        // given
        List<Object[]> rawStats = List.of(
                new Object[]{"종합점수 우수", 600L},
                new Object[]{"종합 점수", 600L},
                new Object[]{"이벤트 미발생", 1700L},
                new Object[]{"이벤트미감지", 1700L},
                new Object[]{"MoBTI 향상", 670L}
        );
        when(rewardRepository.getCurrentYearIssuedGroupedByReason()).thenReturn(rawStats);

        // when
        List<AdminRewardDto.TotalReasonStatsResponse.ReasonStat> result =
                adminRewardService.getTotalIssuedByReason();

        // then
        Map<String, Long> reasonMap = result.stream()
                .collect(Collectors.toMap(AdminRewardDto.TotalReasonStatsResponse.ReasonStat::getReason,
                        AdminRewardDto.TotalReasonStatsResponse.ReasonStat::getCount));

        assertThat(reasonMap.get("종합점수")).isEqualTo(1200L);
        assertThat(reasonMap.get("이벤트미발생")).isEqualTo(3400L);
        assertThat(reasonMap.get("MoBTI향상")).isEqualTo(670L);
    }

    @Test
    @DisplayName("6. 월별 씨앗 지급 통계 조회 - 최근 12개월 데이터")
    void getMonthlyStatsTest() {
        // given
        List<Object[]> rawStats = List.of(
                new Object[]{2024, 4, 12500},
                new Object[]{2024, 5, 13000},
                new Object[]{2024, 6, 12800}
        );
        when(rewardRepository.findMonthlyIssuedStatsLast12Months(any())).thenReturn(rawStats);

        // when
        AdminRewardDto.MonthlyStatsResponse result = adminRewardService.getMonthlyRewardStats();

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
    @DisplayName("7. 최근 씨앗 발급 내역 조회 - 페이징 처리")
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
    @DisplayName("8. 씨앗 필터링 조회 - 이메일, 발급일, 발급사유 필터")
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
                "user54@example.com",
                "종합점수",
                LocalDate.of(2025, 4, 1),
                LocalDate.of(2025, 4, 30),
                pageable
        );

        // then
        assertThat(result.getSearchResult()).hasSize(1);
        assertThat(result.getSearchResult().get(0).getRewardId()).isEqualTo("SEED_1025");
        assertThat(result.getSearchResult().get(0).getAmount()).isEqualTo(5);
        assertThat(result.getPageInfo().getTotalElements()).isEqualTo(40);

        verify(userClient, times(1)).getUserIdByEmail("user54@example.com");
        verify(userClient, times(1)).getEmailByUserId(1L);
    }

    @Test
    @DisplayName("9. 운전별 씨앗 적립 내역 조회")
    void getRewardsByDriveTest() {
        // given
        List<Long> driveIds = List.of(1L, 2L, 3L, 4L);
        AdminRewardDto.RewardsByDriveRequest request = new AdminRewardDto.RewardsByDriveRequest(driveIds);

        when(rewardRepository.sumAmountByDriveId(1L)).thenReturn(Optional.of(100));
        when(rewardRepository.sumAmountByDriveId(2L)).thenReturn(Optional.empty());
        when(rewardRepository.sumAmountByDriveId(3L)).thenReturn(Optional.of(90));
        when(rewardRepository.sumAmountByDriveId(4L)).thenReturn(Optional.of(80));

        // when
        AdminRewardDto.RewardsByDriveResponse result = adminRewardService.getRewardsByDrive(request);

        // then
        assertThat(result.getRewardsByDrive()).hasSize(4);
        assertThat(result.getRewardsByDrive().get(0).getDriveId()).isEqualTo(1L);
        assertThat(result.getRewardsByDrive().get(0).getRewards()).isEqualTo(100);
        assertThat(result.getRewardsByDrive().get(1).getDriveId()).isEqualTo(2L);
        assertThat(result.getRewardsByDrive().get(1).getRewards()).isEqualTo(0);
        assertThat(result.getRewardsByDrive().get(2).getDriveId()).isEqualTo(3L);
        assertThat(result.getRewardsByDrive().get(2).getRewards()).isEqualTo(90);

        verify(rewardRepository, times(4)).sumAmountByDriveId(any());
    }

    @Test
    @DisplayName("변화율 계산 - 이전 값이 0일 때 처리")
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
    @DisplayName("일일 평균 - 사용자가 없을 때 처리")
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
    @DisplayName("월별 통계 - 데이터가 없는 월 처리")
    void testMonthlyStatsWithMissingMonths() {
        // given
        List<Object[]> rawStats = List.of(
                new Object[]{2024, 6, 12500},
                new Object[]{2024, 8, 13000}  // 7월 데이터 없음
        );
        when(rewardRepository.findMonthlyIssuedStatsLast12Months(any())).thenReturn(rawStats);

        // when
        AdminRewardDto.MonthlyStatsResponse result = adminRewardService.getMonthlyRewardStats();

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
    @DisplayName("발급 사유별 통계 - 기타 카테고리 처리")
    void testReasonStatsWithUncategorized() {
        // given
        List<Object[]> rawStats = List.of(
                new Object[]{"종합점수 우수", 1200L},
                new Object[]{"이벤트 미감지", 3400L},
                new Object[]{"MoBTI 개선", 670L},
                new Object[]{"출석 체크", 500L}  // 3가지 카테고리에 속하지 않음
        );
        when(rewardRepository.getCurrentYearIssuedGroupedByReason()).thenReturn(rawStats);

        // when
        List<AdminRewardDto.TotalReasonStatsResponse.ReasonStat> result =
                adminRewardService.getTotalIssuedByReason();

        // then
        assertThat(result).hasSize(3);

        // 출석 체크는 어느 카테고리에도 포함되지 않음
        long totalCount = result.stream().mapToLong(AdminRewardDto.TotalReasonStatsResponse.ReasonStat::getCount).sum();
        assertThat(totalCount).isEqualTo(5270L); // 500L은 제외됨
    }

    @Test
    @DisplayName("필터링 - 필터 파라미터가 null일 때 처리")
    void testFilterWithNullParameters() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Reward> emptyPage = Page.empty(pageable);

        when(rewardRepository.filterRewards(null, null, null, null, pageable))
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
    @DisplayName("운전별 적립 - 리워드가 없는 운전 처리")
    void testRewardsByDriveWithNoRewards() {
        // given
        List<Long> driveIds = List.of(999L);
        AdminRewardDto.RewardsByDriveRequest request = new AdminRewardDto.RewardsByDriveRequest(driveIds);

        when(rewardRepository.sumAmountByDriveId(999L)).thenReturn(Optional.empty());

        // when
        AdminRewardDto.RewardsByDriveResponse result = adminRewardService.getRewardsByDrive(request);

        // then
        assertThat(result.getRewardsByDrive()).hasSize(1);
        assertThat(result.getRewardsByDrive().get(0).getDriveId()).isEqualTo(999L);
        assertThat(result.getRewardsByDrive().get(0).getRewards()).isEqualTo(0);
    }
}