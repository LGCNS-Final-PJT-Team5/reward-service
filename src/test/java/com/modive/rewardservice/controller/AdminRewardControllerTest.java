package com.modive.rewardservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.modive.rewardservice.client.UserClient;
import com.modive.rewardservice.config.UserIdInterceptor;
import com.modive.rewardservice.dto.AdminRewardDto;
import com.modive.rewardservice.service.AdminRewardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.cache.type=none", "eureka.client.enabled=false" })
@AutoConfigureMockMvc
class AdminRewardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdminRewardService adminRewardService;

    @MockBean
    private UserClient userClient;

    // 🔧 이 부분을 추가!
    @MockBean
    private UserIdInterceptor userIdInterceptor;

    private static final String USER_ID = "1";

    @BeforeEach
    void setUp() {
        // 🔧 기존 Mock 설정을 더 구체적으로
        AdminRewardDto.FilteredReward reward = AdminRewardDto.FilteredReward.builder()
                .rewardId("SEED_1025")
                .userId("1")
                .createdAt(LocalDateTime.of(2025, 4, 26, 12, 43, 45))
                .description("종합점수")
                .amount(5)
                .build();

        Page<AdminRewardDto.FilteredReward> filterPage =
                new PageImpl<>(List.of(reward), PageRequest.of(0, 10), 40);

        AdminRewardDto.RewardFilterResponse filterResponse =
                AdminRewardDto.RewardFilterResponse.of(List.of(reward), filterPage);

        // 🔧 더 명확한 Mock 설정
        given(adminRewardService.filterRewards(
                eq("user1@example.com"),  // 구체적 값
                eq("종합점수"),
                eq(LocalDate.of(2025, 4, 1)),
                eq(LocalDate.of(2025, 4, 30)),
                any(Pageable.class)))
                .willReturn(filterResponse);

        // 🔧 또는 모든 경우에 대해 동일한 응답
        given(adminRewardService.filterRewards(any(), any(), any(), any(), any()))
                .willReturn(filterResponse);
    }

    private void setupDefaultMocks() {
        // 기본 통계 Mock
        given(adminRewardService.getTotalIssued()).willReturn(1247890L);
        given(adminRewardService.getChangeRate()).willReturn(3.2);
        given(adminRewardService.getCurrentMonthIssued()).willReturn(20700L);
        given(adminRewardService.getMonthlyChangeRate()).willReturn(12.5);
        given(adminRewardService.getCurrentDailyAverageIssued()).willReturn(730.0);
        given(adminRewardService.getDailyAverageChangeRate()).willReturn(5.8);
        given(adminRewardService.getCurrentPerUserAverageIssued()).willReturn(158.0);
        given(adminRewardService.getPerUserAverageChangeRate()).willReturn(2.1);

        // 사유별 통계 Mock (실제 RewardReason enum 기준)
        List<AdminRewardDto.ReasonStat> defaultStats = List.of(
                AdminRewardDto.ReasonStat.builder()
                        .reason("종합점수").count(800L).ratio(51.6).build(),
                AdminRewardDto.ReasonStat.builder()
                        .reason("이벤트미발생").count(400L).ratio(25.8).build(),
                AdminRewardDto.ReasonStat.builder()
                        .reason("MoBTI향상").count(350L).ratio(22.6).build()
        );

        given(adminRewardService.getTotalRewardStats()).willReturn(
                AdminRewardDto.TotalReasonStatsResponse.of(defaultStats));

        given(adminRewardService.getMonthlyRewardStatsByReason(any())).willReturn(
                AdminRewardDto.MonthlyReasonStatsResponse.of(defaultStats));

        // 월별 트렌드 Mock
        List<AdminRewardDto.MonthlyRewardStat> trendStats = List.of(
                AdminRewardDto.MonthlyRewardStat.builder()
                        .year(2024).month(4).amount(12500).build(),
                AdminRewardDto.MonthlyRewardStat.builder()
                        .year(2024).month(5).amount(13200).build()
        );
        given(adminRewardService.getMonthlyRewardTrends()).willReturn(
                AdminRewardDto.MonthlyStatsResponse.of(trendStats));

        // 히스토리 Mock
        AdminRewardDto.AllRewardHistoryResponse.RewardHistoryItem item =
                AdminRewardDto.AllRewardHistoryResponse.RewardHistoryItem.builder()
                        .rewardId("SEED_1024")
                        .issuedDate(LocalDate.of(2025, 4, 25))
                        .reason("종합점수")
                        .amount(12)
                        .build();
        Page<AdminRewardDto.AllRewardHistoryResponse.RewardHistoryItem> historyPage =
                new PageImpl<>(List.of(item), PageRequest.of(0, 10), 1);
        given(adminRewardService.getAllRewardHistory(any())).willReturn(historyPage);

        // 🔧 수정: 필터/검색 Mock - userId 기반 응답
        AdminRewardDto.FilteredReward reward = AdminRewardDto.FilteredReward.builder()
                .rewardId("SEED_1025")
                .userId("1")  // 🔧 email → userId로 변경
                .createdAt(LocalDateTime.of(2025, 4, 26, 12, 43, 45))
                .description("종합점수")
                .amount(5)
                .build();
        Page<AdminRewardDto.FilteredReward> filterPage = new PageImpl<>(List.of(reward), PageRequest.of(0, 10), 40);
        AdminRewardDto.RewardFilterResponse filterResponse = AdminRewardDto.RewardFilterResponse.of(List.of(reward), filterPage);

        // 🔧 수정: 간소화된 메서드 시그니처에 맞춤
        given(adminRewardService.filterRewards(any(), any(), any(), any(), any())).willReturn(filterResponse);
        given(adminRewardService.searchRewards(any(), any())).willReturn(filterResponse);

        // 운전별 리워드 Mock
        List<AdminRewardDto.DriveReward> driveRewards = List.of(
                AdminRewardDto.DriveReward.builder().driveId("1").rewards(100).build(),
                AdminRewardDto.DriveReward.builder().driveId("2").rewards(0).build()
        );
        given(adminRewardService.getRewardsByDrive(any())).willReturn(
                AdminRewardDto.RewardsByDriveResponse.of(driveRewards));
    }

    @Test
    @DisplayName("1. GET /reward/total-issued - 총 발급 수 조회")
    void getTotalIssued() throws Exception {
        mockMvc.perform(get("/reward/total-issued")
                        .header("X-USER-ID", USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("씨앗 총 발급 수 조회에 성공했습니다."))
                .andExpect(jsonPath("$.data.totalIssued.value").value(1247890))
                .andExpect(jsonPath("$.data.totalIssued.changeRate").value(3.2));
    }

    @Test
    @DisplayName("2. GET /reward/monthly-issued - 월간 발급 수 조회")
    void getMonthlyIssued() throws Exception {
        mockMvc.perform(get("/reward/monthly-issued")
                        .header("X-USER-ID", USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("씨앗 월간 발급 수 조회에 성공했습니다."))
                .andExpect(jsonPath("$.data.monthlyIssued.value").value(20700))
                .andExpect(jsonPath("$.data.monthlyIssued.changeRate").value(12.5));
    }

    @Test
    @DisplayName("3. GET /reward/daily-average-issued - 일 평균 발급 수 조회")
    void getDailyAverageIssued() throws Exception {
        mockMvc.perform(get("/reward/daily-average-issued")
                        .header("X-USER-ID", USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("씨앗 일 평균 발급 수 조회에 성공했습니다."))
                .andExpect(jsonPath("$.data.dailyAverageIssued.value").value(730.0))
                .andExpect(jsonPath("$.data.dailyAverageIssued.changeRate").value(5.8));
    }

    @Test
    @DisplayName("4. GET /reward/per-user-average-issued - 사용자당 평균 발급 수 조회")
    void getPerUserAverageIssued() throws Exception {
        mockMvc.perform(get("/reward/per-user-average-issued")
                        .header("X-USER-ID", USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("씨앗 사용자당 평균 발급 수 조회에 성공했습니다."))
                .andExpect(jsonPath("$.data.perUserAverageIssued.value").value(158.0))
                .andExpect(jsonPath("$.data.perUserAverageIssued.changeRate").value(2.1));
    }

    @Test
    @DisplayName("5. GET /reward/by-reason/total - 발급 사유별 총 통계 조회")
    void getTotalRewardStats() throws Exception {
        mockMvc.perform(get("/reward/by-reason/total")
                        .header("X-USER-ID", USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("발급 사유별 총 통계 조회에 성공했습니다."))
                .andExpect(jsonPath("$.data.totalRewardStatistics[0].reason").value("종합점수"))
                .andExpect(jsonPath("$.data.totalRewardStatistics[0].count").value(800))
                .andExpect(jsonPath("$.data.totalRewardStatistics[0].ratio").value(51.6));
    }

    @Test
    @DisplayName("5-2. GET /reward/by-reason/monthly - 발급 사유별 월별 통계 조회 (현재 월)")
    void getMonthlyRewardStatsByReasonCurrentMonth() throws Exception {
        mockMvc.perform(get("/reward/by-reason/monthly")
                        .header("X-USER-ID", USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("발급 사유별 월별 통계 조회에 성공했습니다."))
                .andExpect(jsonPath("$.data.monthlyRewardStatistics[0].reason").value("종합점수"))
                .andExpect(jsonPath("$.data.monthlyRewardStatistics[0].count").value(800))
                .andExpect(jsonPath("$.data.monthlyRewardStatistics[0].ratio").value(51.6));
    }

    @Test
    @DisplayName("5-3. GET /reward/by-reason/monthly?month=2025-04 - 발급 사유별 월별 통계 조회 (특정 월)")
    void getMonthlyRewardStatsByReasonWithParam() throws Exception {
        // given - 특정 월에 대한 Mock 설정
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
                        .param("month", "2025-04")
                        .header("X-USER-ID", USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("발급 사유별 월별 통계 조회에 성공했습니다."))
                .andExpect(jsonPath("$.data.monthlyRewardStatistics[0].reason").value("종합점수"))
                .andExpect(jsonPath("$.data.monthlyRewardStatistics[0].count").value(650))
                .andExpect(jsonPath("$.data.monthlyRewardStatistics[0].ratio").value(48.1));
    }

    @Test
    @DisplayName("6. GET /reward/monthly-stats - 월별 씨앗 지급 통계 조회")
    void getMonthlyRewardTrends() throws Exception {
        mockMvc.perform(get("/reward/monthly-stats")
                        .header("X-USER-ID", USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("월별 지급 통계 조회에 성공했습니다."))
                .andExpect(jsonPath("$.data.monthlyRewardStatistics[0].year").value(2024))
                .andExpect(jsonPath("$.data.monthlyRewardStatistics[0].month").value(4))
                .andExpect(jsonPath("$.data.monthlyRewardStatistics[0].amount").value(12500));
    }

    @Test
    @DisplayName("7. GET /reward/history - 최근 씨앗 발급 내역 조회")
    void getAllRewardHistory() throws Exception {
        mockMvc.perform(get("/reward/history")
                        .param("page", "0")
                        .param("size", "10")
                        .header("X-USER-ID", USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("전체 씨앗 발급 내역 조회에 성공했습니다."))
                .andExpect(jsonPath("$.data.rewardHistory[0].rewardId").value("SEED_1024"))
                .andExpect(jsonPath("$.data.rewardHistory[0].issuedDate").value("2025-04-25"))
                .andExpect(jsonPath("$.data.rewardHistory[0].reason").value("종합점수"))
                .andExpect(jsonPath("$.data.rewardHistory[0].amount").value(12));
    }

    @Test
    @DisplayName("8. GET /reward/filter - 씨앗 필터링 조회")
    void filterRewards() throws Exception {
        mockMvc.perform(get("/reward/filter")
                        .param("email", "user1@example.com")
                        .param("description", "종합점수")
                        .param("startDate", "2025-04-01")
                        .param("endDate", "2025-04-30")
                        .header("X-USER-ID", USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("씨앗 발급 내역 검색에 성공했습니다."))
                .andExpect(jsonPath("$.data.searchResult[0].rewardId").value("SEED_1025"))
                .andExpect(jsonPath("$.data.searchResult[0].userId").value("1"))  // 🔧 email → userId
                .andExpect(jsonPath("$.data.pageInfo.currentPage").value(1))
                .andExpect(jsonPath("$.data.pageInfo.totalElements").value(40));
    }

    @Test
    @DisplayName("8-2. GET /reward/filter - 씨앗 고급 검색 조회")
    void searchRewards() throws Exception {
        mockMvc.perform(get("/reward/filter")  // 🔧 POST → GET으로 변경
                        .header("X-USER-ID", USER_ID)
                        .param("email", "user1@example.com")           // 🔧 Body → Query Parameter
                        .param("description", "종합점수")
                        .param("startDate", "2025-04-01")
                        .param("endDate", "2025-04-30")
                        .param("minAmount", "1")
                        .param("maxAmount", "100"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("씨앗 발급 내역 검색에 성공했습니다."))
                .andExpect(jsonPath("$.data.searchResult[0].rewardId").value("SEED_1025"))
                .andExpect(jsonPath("$.data.searchResult[0].userId").value("1"));
    }

    @Test
    @DisplayName("9. POST /reward/by-drive - 운전별 씨앗 적립 내역 조회")
    void getRewardsByDrive() throws Exception {
        AdminRewardDto.RewardsByDriveRequest request = new AdminRewardDto.RewardsByDriveRequest(List.of("1", "2", "3", "4"));

        mockMvc.perform(post("/reward/by-drive")
                        .header("X-USER-ID", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("운전별 씨앗 적립 내역 조회에 성공하였습니다."))
                .andExpect(jsonPath("$.data.rewardsByDrive[0].driveId").value(1))
                .andExpect(jsonPath("$.data.rewardsByDrive[0].rewards").value(100));
    }

    // 🔧 추가: 에러 케이스 테스트
    @Test
    @DisplayName("10. GET /reward/filter - 존재하지 않는 사용자 이메일로 필터링")
    void filterRewardsWithNonExistentEmail() throws Exception {
        // given - 존재하지 않는 사용자
        given(userClient.getUserIdByEmail("nonexistent@example.com")).willReturn(null);
        given(adminRewardService.filterRewards(any(), any(), any(), any(), any()))
                .willReturn(AdminRewardDto.RewardFilterResponse.empty());

        mockMvc.perform(get("/reward/filter")
                        .param("email", "nonexistent@example.com")
                        .header("X-USER-ID", USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.searchResult").isEmpty())
                .andExpect(jsonPath("$.data.pageInfo.totalElements").value(0));
    }

    @Test
    @DisplayName("11. GET /reward/filter - 검증 실패 케이스")
    void searchRewardsValidationFailure() throws Exception {
        mockMvc.perform(get("/reward/filter")  // 🔧 POST → GET으로 변경
                        .header("X-USER-ID", USER_ID)
                        .param("email", "user1@example.com")
                        .param("startDate", "2025-04-30")      // 잘못된 날짜
                        .param("endDate", "2025-04-01"))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("12. GET /reward/by-reason/monthly - 잘못된 월 형식")
    void getMonthlyRewardStatsByReasonInvalidFormat() throws Exception {
        // given - 잘못된 형식에 대해 빈 결과 반환하도록 Mock
        given(adminRewardService.getMonthlyRewardStatsByReason("invalid-format")).willReturn(
                AdminRewardDto.MonthlyReasonStatsResponse.of(List.of()));

        mockMvc.perform(get("/reward/by-reason/monthly")
                        .param("month", "invalid-format")
                        .header("X-USER-ID", USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.monthlyRewardStatistics").isEmpty());
    }
}