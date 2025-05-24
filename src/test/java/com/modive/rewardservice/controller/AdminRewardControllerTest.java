package com.modive.rewardservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.modive.rewardservice.client.UserClient;
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

@SpringBootTest
@AutoConfigureMockMvc
class AdminRewardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdminRewardService adminRewardService;

    @MockBean
    private UserClient userClient;  // UserClient Mock 추가

    private static final String USER_ID = "1";

    @BeforeEach
    void setUp() {
        // UserClient Mock 설정
        given(userClient.getUserIdByEmail(anyString())).willReturn(1L);
        given(userClient.getEmailByUserId(anyLong())).willReturn("user@example.com");
    }

    @Test
    @DisplayName("1. GET /reward/total-issued - 총 발급 수 조회")
    void getTotalIssued() throws Exception {
        // given
        given(adminRewardService.getTotalIssued()).willReturn(1247890L);
        given(adminRewardService.getChangeRate()).willReturn(3.2);

        // when & then
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
        // given
        given(adminRewardService.getCurrentMonthIssued()).willReturn(20700L);
        given(adminRewardService.getMonthlyChangeRate()).willReturn(12.5);

        // when & then
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
        // given
        given(adminRewardService.getCurrentDailyAverageIssued()).willReturn(730.0);
        given(adminRewardService.getDailyAverageChangeRate()).willReturn(5.8);

        // when & then
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
        // given
        given(adminRewardService.getCurrentPerUserAverageIssued()).willReturn(158.0);
        given(adminRewardService.getPerUserAverageChangeRate()).willReturn(2.1);

        // when & then
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
    void getTotalIssuedByReason() throws Exception {
        // given
        List<AdminRewardDto.TotalReasonStatsResponse.ReasonStat> stats = List.of(
                AdminRewardDto.TotalReasonStatsResponse.ReasonStat.builder()
                        .reason("종합점수").count(1200L).ratio(51.6).build(),
                AdminRewardDto.TotalReasonStatsResponse.ReasonStat.builder()
                        .reason("이벤트미발생").count(3400L).ratio(20.0).build(),
                AdminRewardDto.TotalReasonStatsResponse.ReasonStat.builder()
                        .reason("MoBTI향상").count(670L).ratio(5.0).build()
        );
        given(adminRewardService.getTotalIssuedByReason()).willReturn(stats);

        // when & then
        mockMvc.perform(get("/reward/by-reason/total")
                        .header("X-USER-ID", USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("리워드 발급 사유별 통계 조회에 성공했습니다."))
                .andExpect(jsonPath("$.data.totalRewardStatistics[0].reason").value("종합점수"))
                .andExpect(jsonPath("$.data.totalRewardStatistics[0].count").value(1200))
                .andExpect(jsonPath("$.data.totalRewardStatistics[0].ratio").value(51.6));
    }

    @Test
    @DisplayName("6. GET /reward/monthly-stats - 월별 씨앗 지급 통계 조회")
    void getMonthlyStats() throws Exception {
        // given
        List<AdminRewardDto.MonthlyRewardStat> stats = List.of(
                AdminRewardDto.MonthlyRewardStat.builder()
                        .year(2024).month(4).amount(12500).build(),
                AdminRewardDto.MonthlyRewardStat.builder()
                        .year(2024).month(5).amount(12500).build()
        );
        AdminRewardDto.MonthlyStatsResponse response = AdminRewardDto.MonthlyStatsResponse.of(stats);
        given(adminRewardService.getMonthlyRewardStats()).willReturn(response);

        // when & then
        mockMvc.perform(get("/reward/monthly-stats")
                        .header("X-USER-ID", USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("월별 씨앗 지급 통계 조회에 성공하였습니다."))
                .andExpect(jsonPath("$.data.monthlyRewardStatistics[0].year").value(2024))
                .andExpect(jsonPath("$.data.monthlyRewardStatistics[0].month").value(4))
                .andExpect(jsonPath("$.data.monthlyRewardStatistics[0].amount").value(12500));
    }

    @Test
    @DisplayName("7. GET /reward/history/all - 최근 씨앗 발급 내역 조회")
    void getAllRewardHistory() throws Exception {
        // given
        AdminRewardDto.AllRewardHistoryResponse.RewardHistoryItem item =
                AdminRewardDto.AllRewardHistoryResponse.RewardHistoryItem.builder()
                        .rewardId("SEED_1024")
                        .issuedDate(LocalDate.of(2025, 4, 25))
                        .reason("종합점수")
                        .amount(12)
                        .build();

        Page<AdminRewardDto.AllRewardHistoryResponse.RewardHistoryItem> page =
                new PageImpl<>(List.of(item), PageRequest.of(0, 10), 1);

        given(adminRewardService.getAllRewardHistory(any())).willReturn(page);

        // when & then
        mockMvc.perform(get("/reward/history/all")
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
        // given
        AdminRewardDto.FilteredReward reward = AdminRewardDto.FilteredReward.builder()
                .rewardId("SEED_1025")
                .email("user54@example.com")
                .createdAt(LocalDateTime.of(2025, 4, 26, 12, 43, 45))
                .description("종합점수")
                .amount(5)
                .build();

        Page<AdminRewardDto.FilteredReward> page = new PageImpl<>(List.of(reward), PageRequest.of(0, 10), 40);
        AdminRewardDto.RewardFilterResponse response = AdminRewardDto.RewardFilterResponse.of(List.of(reward), page);

        given(adminRewardService.filterRewards(any(), any(), any(), any(), any())).willReturn(response);

        // when & then
        mockMvc.perform(get("/reward/filter")
                        .param("email", "user54@example.com")
                        .param("description", "종합점수")
                        .param("startDate", "2025-04-01")
                        .param("endDate", "2025-04-30")
                        .header("X-USER-ID", USER_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("씨앗 발급 내역 검색에 성공했습니다."))
                .andExpect(jsonPath("$.data.searchResult[0].rewardId").value("SEED_1025"))
                .andExpect(jsonPath("$.data.searchResult[0].email").value("user54@example.com"))
                .andExpect(jsonPath("$.data.pageInfo.currentPage").value(1))
                .andExpect(jsonPath("$.data.pageInfo.totalElements").value(40));
    }

    @Test
    @DisplayName("9. POST /reward/by-drive - 운전별 씨앗 적립 내역 조회")
    void getRewardsByDrive() throws Exception {
        // given
        AdminRewardDto.RewardsByDriveRequest request = new AdminRewardDto.RewardsByDriveRequest(List.of(1L, 2L, 3L, 4L));

        List<AdminRewardDto.DriveReward> rewards = List.of(
                AdminRewardDto.DriveReward.builder().driveId(1L).rewards(100).build(),
                AdminRewardDto.DriveReward.builder().driveId(2L).rewards(0).build(),
                AdminRewardDto.DriveReward.builder().driveId(3L).rewards(90).build(),
                AdminRewardDto.DriveReward.builder().driveId(4L).rewards(80).build()
        );
        AdminRewardDto.RewardsByDriveResponse response = AdminRewardDto.RewardsByDriveResponse.of(rewards);

        given(adminRewardService.getRewardsByDrive(any())).willReturn(response);

        // when & then
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
                .andExpect(jsonPath("$.data.rewardsByDrive[0].rewards").value(100))
                .andExpect(jsonPath("$.data.rewardsByDrive[1].driveId").value(2))
                .andExpect(jsonPath("$.data.rewardsByDrive[1].rewards").value(0));
    }
}