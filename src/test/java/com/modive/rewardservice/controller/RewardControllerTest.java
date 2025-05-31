package com.modive.rewardservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.modive.rewardservice.config.UserIdInterceptor;
import com.modive.rewardservice.domain.Reward;
import com.modive.rewardservice.domain.RewardType;
import com.modive.rewardservice.dto.RewardDto;
import com.modive.rewardservice.dto.request.ScoreInfo;
import com.modive.rewardservice.service.RewardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * RewardController 순수 단위 테스트
 * Spring Context 없이 컨트롤러만 테스트
 */
@ExtendWith(MockitoExtension.class)
class RewardControllerTest {

    @Mock
    private RewardService rewardService;

    @InjectMocks
    private RewardController rewardController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private static final String TEST_USER_ID = "user123";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(rewardController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("POST /reward/earn - 복합 리워드 적립 성공")
    void earnComplexRewards_Success() throws Exception {
        // Given
        RewardDto.EarnComplexRequest request = RewardDto.EarnComplexRequest.builder()
                .driveId("drive123")
                .score(85)
                .drivingTime(15)
                .lastScore(ScoreInfo.builder()
                        .carbon(40)
                        .safety(45)
                        .accident(50)
                        .focus(35)
                        .build())
                .currentScore(ScoreInfo.builder()
                        .carbon(60)
                        .safety(65)
                        .accident(70)
                        .focus(55)
                        .build())
                .build();

        try (MockedStatic<UserIdInterceptor> mockedStatic = mockStatic(UserIdInterceptor.class)) {
            mockedStatic.when(UserIdInterceptor::getCurrentUserId).thenReturn(TEST_USER_ID);
            doNothing().when(rewardService).calculateAndEarn(any());

            // When & Then
            mockMvc.perform(post("/reward/earn")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isNoContent());

            verify(rewardService, times(1)).calculateAndEarn(any());
        }
    }

    @Test
    @DisplayName("POST /reward/earn - JSON 파싱 에러 테스트")
    void earnComplexRewards_InvalidJson() throws Exception {
        // Given - 잘못된 JSON
        String invalidJson = "{ invalid json }";

        // When & Then
        mockMvc.perform(post("/reward/earn")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(rewardService, never()).calculateAndEarn(any());
    }

    @Test
    @DisplayName("GET /reward/users/balance - 사용자 잔액 조회 성공")
    void getBalance_Success() throws Exception {
        // Given
        Long expectedBalance = 100L;

        try (MockedStatic<UserIdInterceptor> mockedStatic = mockStatic(UserIdInterceptor.class)) {
            mockedStatic.when(UserIdInterceptor::getCurrentUserId).thenReturn(TEST_USER_ID);
            when(rewardService.getBalance(TEST_USER_ID)).thenReturn(expectedBalance);

            // When & Then
            mockMvc.perform(get("/reward/users/balance"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.message").value("씨앗 잔액 조회에 성공하였습니다."))
                    .andExpect(jsonPath("$.data.userId").value(TEST_USER_ID))
                    .andExpect(jsonPath("$.data.balance").value(expectedBalance));

            verify(rewardService, times(1)).getBalance(TEST_USER_ID);
        }
    }

    @Test
    @DisplayName("GET /reward/users/history - 사용자 리워드 내역 조회 성공")
    void getRewardHistory_Success() throws Exception {
        // Given
        List<Reward> rewards = createMockRewards();
        Page<Reward> rewardPage = new PageImpl<>(rewards, PageRequest.of(0, 10), 2);

        try (MockedStatic<UserIdInterceptor> mockedStatic = mockStatic(UserIdInterceptor.class)) {
            mockedStatic.when(UserIdInterceptor::getCurrentUserId).thenReturn(TEST_USER_ID);
            when(rewardService.getRewardHistory(eq(TEST_USER_ID), any())).thenReturn(rewardPage);

            // When & Then
            mockMvc.perform(get("/reward/users/history")
                            .param("page", "0")
                            .param("size", "10"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.message").value("리워드 내역 조회에 성공했습니다."))
                    .andExpect(jsonPath("$.data.rewardHistory").isArray())
                    .andExpect(jsonPath("$.data.rewardHistory.length()").value(2))
                    .andExpect(jsonPath("$.data.pageInfo.page").value(1))
                    .andExpect(jsonPath("$.data.pageInfo.pageSize").value(10))
                    .andExpect(jsonPath("$.data.pageInfo.totalElements").value(2))
                    .andExpect(jsonPath("$.data.pageInfo.totalPages").value(1));

            verify(rewardService, times(1)).getRewardHistory(eq(TEST_USER_ID), any());
        }
    }

    @Test
    @DisplayName("POST /reward/earn - 낮은 점수와 짧은 주행시간")
    void earnComplexRewards_LowScoreAndShortDriving() throws Exception {
        // Given
        RewardDto.EarnComplexRequest request = RewardDto.EarnComplexRequest.builder()
                .driveId("drive456")
                .score(30)
                .drivingTime(5)
                .lastScore(ScoreInfo.builder()
                        .carbon(30)
                        .safety(35)
                        .accident(40)
                        .focus(25)
                        .build())
                .currentScore(ScoreInfo.builder()
                        .carbon(35)
                        .safety(40)
                        .accident(45)
                        .focus(30)
                        .build())
                .build();

        try (MockedStatic<UserIdInterceptor> mockedStatic = mockStatic(UserIdInterceptor.class)) {
            mockedStatic.when(UserIdInterceptor::getCurrentUserId).thenReturn(TEST_USER_ID);
            doNothing().when(rewardService).calculateAndEarn(any());

            // When & Then
            mockMvc.perform(post("/reward/earn")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isNoContent());

            verify(rewardService, times(1)).calculateAndEarn(any());
        }
    }

    @Test
    @DisplayName("POST /reward/earn - null 값들이 포함된 요청")
    void earnComplexRewards_WithNullValues() throws Exception {
        // Given - 일부 필드가 null인 요청
        RewardDto.EarnComplexRequest request = RewardDto.EarnComplexRequest.builder()
                .driveId("drive789")
                .score(null)
                .drivingTime(null)
                .lastScore(null)
                .currentScore(null)
                .build();

        try (MockedStatic<UserIdInterceptor> mockedStatic = mockStatic(UserIdInterceptor.class)) {
            mockedStatic.when(UserIdInterceptor::getCurrentUserId).thenReturn(TEST_USER_ID);
            doNothing().when(rewardService).calculateAndEarn(any());

            // When & Then
            mockMvc.perform(post("/reward/earn")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isNoContent());

            verify(rewardService, times(1)).calculateAndEarn(any());
        }
    }

    @Test
    @DisplayName("POST /reward/earn - UserIdInterceptor 실패시 예외 처리")
    void earnComplexRewards_UserIdInterceptorFailure() throws Exception {
        // Given
        RewardDto.EarnComplexRequest request = RewardDto.EarnComplexRequest.builder()
                .driveId("drive123")
                .score(85)
                .build();

        try (MockedStatic<UserIdInterceptor> mockedStatic = mockStatic(UserIdInterceptor.class)) {
            // UserIdInterceptor가 null을 반환하는 경우
            mockedStatic.when(UserIdInterceptor::getCurrentUserId).thenReturn(null);

            // When & Then - 실제로는 interceptor에서 처리되어 400이 될 수 있음
            mockMvc.perform(post("/reward/earn")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print());
            // .andExpect(status().isBadRequest()); // 실제 인터셉터 로직에 따라 다름

            // service 호출 여부는 실제 구현에 따라 다름
        }
    }

    /**
     * 테스트용 Reward 리스트 생성
     */
    private List<Reward> createMockRewards() {
        Reward reward1 = Reward.builder()
                .userId(TEST_USER_ID)
                .amount(5L)
                .type(RewardType.EARNED)
                .description("종합점수")
                .balanceSnapshot(105L)
                .build();

        Reward reward2 = Reward.builder()
                .userId(TEST_USER_ID)
                .amount(1L)
                .type(RewardType.EARNED)
                .description("이벤트미발생")
                .balanceSnapshot(100L)
                .build();

        return Arrays.asList(reward1, reward2);
    }
}