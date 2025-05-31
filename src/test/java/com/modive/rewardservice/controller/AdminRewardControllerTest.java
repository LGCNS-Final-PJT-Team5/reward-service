package com.modive.rewardservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.modive.rewardservice.dto.AdminRewardDto;
import com.modive.rewardservice.service.AdminRewardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminRewardControllerTest {

    @Mock
    private AdminRewardService adminRewardService;

    @InjectMocks
    private AdminRewardController adminRewardController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); // 날짜를 문자열로 직렬화

        mockMvc = MockMvcBuilders.standaloneSetup(adminRewardController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    @DisplayName("1. GET /reward/stats/total - 총 발급 수 조회")
    void getTotalStats() throws Exception {
        // given
        given(adminRewardService.getTotalIssued()).willReturn(1247890L);
        given(adminRewardService.getChangeRate()).willReturn(3.2);

        mockMvc.perform(get("/reward/stats/total"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("씨앗 총 발급 수 조회에 성공했습니다."))
                .andExpect(jsonPath("$.data.totalIssued.value").value(1247890))
                .andExpect(jsonPath("$.data.totalIssued.changeRate").value(3.2));
    }

    @Test
    @DisplayName("2. GET /reward/stats/monthly - 월간 발급 수 조회")
    void getMonthlyStats() throws Exception {
        // given
        given(adminRewardService.getCurrentMonthIssued()).willReturn(20700L);
        given(adminRewardService.getMonthlyChangeRate()).willReturn(12.5);

        mockMvc.perform(get("/reward/stats/monthly"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("씨앗 월간 발급 수 조회에 성공했습니다."))
                .andExpect(jsonPath("$.data.monthlyIssued.value").value(20700))
                .andExpect(jsonPath("$.data.monthlyIssued.changeRate").value(12.5));
    }

    @Test
    @DisplayName("3. GET /reward/stats/daily - 일 평균 발급 수 조회")
    void getDailyStats() throws Exception {
        // given
        given(adminRewardService.getCurrentDailyAverageIssued()).willReturn(730.0);
        given(adminRewardService.getDailyAverageChangeRate()).willReturn(5.8);

        mockMvc.perform(get("/reward/stats/daily"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("씨앗 일 평균 발급 수 조회에 성공했습니다."))
                .andExpect(jsonPath("$.data.dailyAverageIssued.value").value(730.0))
                .andExpect(jsonPath("$.data.dailyAverageIssued.changeRate").value(5.8));
    }

    @Test
    @DisplayName("4. GET /reward/stats/per-user - 사용자당 평균 발급 수 조회")
    void getPerUserStats() throws Exception {
        // given
        given(adminRewardService.getCurrentPerUserAverageIssued()).willReturn(158.0);
        given(adminRewardService.getPerUserAverageChangeRate()).willReturn(2.1);

        mockMvc.perform(get("/reward/stats/per-user"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("씨앗 사용자당 평균 발급 수 조회에 성공했습니다."))
                .andExpect(jsonPath("$.data.perUserAverageIssued.value").value(158.0))
                .andExpect(jsonPath("$.data.perUserAverageIssued.changeRate").value(2.1));
    }

    @Test
    @DisplayName("5. GET /reward/by-reason/total - 발급 사유별 총 통계 조회")
    void getTotalReasonStats() throws Exception {
        // given
        List<AdminRewardDto.ReasonStat> stats = List.of(
                AdminRewardDto.ReasonStat.builder()
                        .reason("종합점수").count(800L).ratio(51.6).build(),
                AdminRewardDto.ReasonStat.builder()
                        .reason("이벤트미발생").count(400L).ratio(25.8).build(),
                AdminRewardDto.ReasonStat.builder()
                        .reason("MoBTI향상").count(350L).ratio(22.6).build()
        );
        given(adminRewardService.getTotalRewardStats()).willReturn(
                AdminRewardDto.TotalReasonStatsResponse.of(stats));

        mockMvc.perform(get("/reward/by-reason/total"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("리워드 발급 사유별 통계 조회에 성공했습니다."))
                .andExpect(jsonPath("$.data.totalRewardStatistics[0].reason").value("종합점수"))
                .andExpect(jsonPath("$.data.totalRewardStatistics[0].count").value(800))
                .andExpect(jsonPath("$.data.totalRewardStatistics[0].ratio").value(51.6));
    }

    @Test
    @DisplayName("6. GET /reward/by-reason/monthly - 발급 사유별 월별 통계 조회 (현재 월)")
    void getMonthlyReasonStatsCurrentMonth() throws Exception {
        // given
        List<AdminRewardDto.ReasonStat> stats = List.of(
                AdminRewardDto.ReasonStat.builder()
                        .reason("종합점수").count(800L).ratio(51.6).build(),
                AdminRewardDto.ReasonStat.builder()
                        .reason("이벤트미발생").count(400L).ratio(25.8).build(),
                AdminRewardDto.ReasonStat.builder()
                        .reason("MoBTI향상").count(350L).ratio(22.6).build()
        );
        given(adminRewardService.getMonthlyRewardStatsByReason(null)).willReturn(
                AdminRewardDto.MonthlyReasonStatsResponse.of(stats));

        mockMvc.perform(get("/reward/by-reason/monthly"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("리워드 발급 사유별 월별 통계 조회에 성공했습니다."))
                .andExpect(jsonPath("$.data.monthlyRewardStatistics[0].reason").value("종합점수"))
                .andExpect(jsonPath("$.data.monthlyRewardStatistics[0].count").value(800))
                .andExpect(jsonPath("$.data.monthlyRewardStatistics[0].ratio").value(51.6));
    }

    @Test
    @DisplayName("7. GET /reward/by-reason/monthly?month=2025-04 - 발급 사유별 월별 통계 조회 (특정 월)")
    void getMonthlyReasonStatsWithParam() throws Exception {
        // given
        List<AdminRewardDto.ReasonStat> specificMonthStats = List.of(
                AdminRewardDto.ReasonStat.builder()
                        .reason("종합점수").count(650L).ratio(48.1).build(),
                AdminRewardDto.ReasonStat.builder()
                        .reason("이벤트미발생").count(350L).ratio(25.9).build(),
                AdminRewardDto.ReasonStat.builder()
                        .reason("MoBTI향상").count(350L).ratio(25.9).build()
        );
        given(adminRewardService.getMonthlyRewardStatsByReason("2025-04")).willReturn(
                AdminRewardDto.MonthlyReasonStatsResponse.of(specificMonthStats));

        mockMvc.perform(get("/reward/by-reason/monthly")
                        .param("month", "2025-04"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("리워드 발급 사유별 월별 통계 조회에 성공했습니다."))
                .andExpect(jsonPath("$.data.monthlyRewardStatistics[0].reason").value("종합점수"))
                .andExpect(jsonPath("$.data.monthlyRewardStatistics[0].count").value(650))
                .andExpect(jsonPath("$.data.monthlyRewardStatistics[0].ratio").value(48.1));
    }

    @Test
    @DisplayName("8. GET /reward/monthly-stats - 월별 씨앗 지급 통계 조회")
    void getMonthlyHistoryStats() throws Exception {
        // given
        List<AdminRewardDto.MonthlyRewardStat> trendStats = List.of(
                AdminRewardDto.MonthlyRewardStat.builder()
                        .year(2024).month(4).amount(12500).build(),
                AdminRewardDto.MonthlyRewardStat.builder()
                        .year(2024).month(5).amount(13200).build()
        );
        given(adminRewardService.getMonthlyRewardTrends()).willReturn(
                AdminRewardDto.MonthlyStatsResponse.of(trendStats));

        mockMvc.perform(get("/reward/monthly-stats"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("월별 씨앗 지급 통계 조회에 성공하였습니다."))
                .andExpect(jsonPath("$.data.monthlyRewardStatistics[0].year").value(2024))
                .andExpect(jsonPath("$.data.monthlyRewardStatistics[0].month").value(4))
                .andExpect(jsonPath("$.data.monthlyRewardStatistics[0].amount").value(12500));
    }

    @Test
    @DisplayName("9. GET /reward/history/all - 최근 씨앗 발급 내역 조회")
    void getAllRewardHistory() throws Exception {
        // given
        AdminRewardDto.AllRewardHistoryResponse.RewardHistoryItem item =
                AdminRewardDto.AllRewardHistoryResponse.RewardHistoryItem.builder()
                        .rewardId("SEED_1024")
                        .issuedDate(LocalDate.of(2025, 4, 25))
                        .reason("종합점수")
                        .amount(12)
                        .build();
        Page<AdminRewardDto.AllRewardHistoryResponse.RewardHistoryItem> historyPage =
                new PageImpl<>(List.of(item), PageRequest.of(0, 10), 1);
        given(adminRewardService.getAllRewardHistory(any(Pageable.class))).willReturn(historyPage);

        mockMvc.perform(get("/reward/history/all")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("전체 씨앗 발급 내역 조회에 성공했습니다."))
                .andExpect(jsonPath("$.data.rewardHistory[0].rewardId").value("SEED_1024"))
                .andExpect(jsonPath("$.data.rewardHistory[0].issuedDate[0]").value(2025))  // 년
                .andExpect(jsonPath("$.data.rewardHistory[0].issuedDate[1]").value(4))     // 월
                .andExpect(jsonPath("$.data.rewardHistory[0].issuedDate[2]").value(25))    // 일
                .andExpect(jsonPath("$.data.rewardHistory[0].reason").value("종합점수"))
                .andExpect(jsonPath("$.data.rewardHistory[0].amount").value(12));
    }

    @Test
    @DisplayName("10. GET /reward/filter - 씨앗 필터링 조회")
    void filterRewardHistory() throws Exception {
        // given
        AdminRewardDto.FilteredReward reward = AdminRewardDto.FilteredReward.builder()
                .rewardId("SEED_1025")
                .userId("test-user-id")
                .createdAt(LocalDateTime.of(2025, 4, 26, 12, 43, 45))
                .description("종합점수")
                .amount(5)
                .build();
        Page<AdminRewardDto.FilteredReward> filterPage =
                new PageImpl<>(List.of(reward), PageRequest.of(0, 10), 40);
        AdminRewardDto.RewardFilterResponse filterResponse =
                AdminRewardDto.RewardFilterResponse.of(List.of(reward), filterPage);

        given(adminRewardService.filterRewards(
                eq("user1@example.com"),
                eq("종합점수"),
                eq(LocalDate.of(2025, 4, 1)),
                eq(LocalDate.of(2025, 4, 30)),
                any(Pageable.class))).willReturn(filterResponse);

        mockMvc.perform(get("/reward/filter")
                        .param("email", "user1@example.com")
                        .param("description", "종합점수")
                        .param("startDate", "2025-04-01")
                        .param("endDate", "2025-04-30"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("씨앗 발급 내역 검색에 성공했습니다."))
                .andExpect(jsonPath("$.data.searchResult[0].rewardId").value("SEED_1025"))
                .andExpect(jsonPath("$.data.searchResult[0].userId").value("test-user-id"))
                .andExpect(jsonPath("$.data.pageInfo.currentPage").value(1))
                .andExpect(jsonPath("$.data.pageInfo.totalElements").value(40));
    }

    @Test
    @DisplayName("11. POST /reward/by-drive - 운전별 씨앗 적립 내역 조회")
    void getRewardHistoryByDrive() throws Exception {
        // given
        AdminRewardDto.RewardsByDriveRequest request =
                new AdminRewardDto.RewardsByDriveRequest(List.of("1", "2", "3", "4"));

        List<AdminRewardDto.DriveReward> driveRewards = List.of(
                AdminRewardDto.DriveReward.builder().driveId("1").rewards(100).build(),
                AdminRewardDto.DriveReward.builder().driveId("2").rewards(0).build()
        );
        given(adminRewardService.getRewardsByDrive(any(AdminRewardDto.RewardsByDriveRequest.class)))
                .willReturn(AdminRewardDto.RewardsByDriveResponse.of(driveRewards));

        mockMvc.perform(post("/reward/by-drive")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("운전별 씨앗 적립 내역 조회에 성공하였습니다."))
                .andExpect(jsonPath("$.data.rewardsByDrive[0].driveId").value("1"))
                .andExpect(jsonPath("$.data.rewardsByDrive[0].rewards").value(100));
    }

    // ===== 에러 케이스 테스트 =====

    @Test
    @DisplayName("12. GET /reward/filter - 존재하지 않는 사용자 이메일로 필터링")
    void filterRewardsWithNonExistentEmail() throws Exception {
        // given - 존재하지 않는 사용자에 대해 빈 결과 반환
        given(adminRewardService.filterRewards(
                eq("nonexistent@example.com"), any(), any(), any(), any()))
                .willReturn(AdminRewardDto.RewardFilterResponse.empty());

        mockMvc.perform(get("/reward/filter")
                        .param("email", "nonexistent@example.com"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.searchResult").isEmpty())
                .andExpect(jsonPath("$.data.pageInfo.totalElements").value(0));
    }

    @Test
    @DisplayName("13. GET /reward/filter - 잘못된 날짜 범위")
    void filterRewardsWithInvalidDateRange() throws Exception {
        // given - 실제로 잘못된 날짜 범위

        mockMvc.perform(get("/reward/filter")
                        .param("startDate", "2025-04-30")
                        .param("endDate", "2025-04-01"))
                .andDo(print())
                .andExpect(status().isOk()) // 일단 200이 나오는지 확인
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue())); // data가 null인지 확인
    }

    @Test
    @DisplayName("14. GET /reward/by-reason/monthly - 잘못된 월 형식")
    void getMonthlyRewardStatsByReasonInvalidFormat() throws Exception {
        // given - 잘못된 형식에 대해 빈 결과 반환
        given(adminRewardService.getMonthlyRewardStatsByReason("invalid-format")).willReturn(
                AdminRewardDto.MonthlyReasonStatsResponse.of(List.of()));

        mockMvc.perform(get("/reward/by-reason/monthly")
                        .param("month", "invalid-format"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.monthlyRewardStatistics").isEmpty());
    }
}