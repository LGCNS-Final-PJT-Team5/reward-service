
package com.modive.rewardservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.modive.rewardservice.config.UserIdInterceptor;
import com.modive.rewardservice.domain.Reward;
import com.modive.rewardservice.domain.RewardType;
import com.modive.rewardservice.dto.RewardDto;
import com.modive.rewardservice.dto.request.ScoreInfo;
import com.modive.rewardservice.service.RewardService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RewardController.class)
class RewardControllerIntegratedTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RewardService rewardService;

    @Autowired
    private ObjectMapper objectMapper;

    @WithMockUser(username = "testUser")
    @Test
    @DisplayName("POST /reward/earn - 복합 리워드 적립 성공")
    void earnRewardsSuccessfully() throws Exception {
        try (MockedStatic<UserIdInterceptor> mockedStatic = mockStatic(UserIdInterceptor.class)) {
            mockedStatic.when(UserIdInterceptor::getCurrentUserId).thenReturn(1L);

            RewardDto.EarnComplexRequest request = RewardDto.EarnComplexRequest.builder()
                    .driveId(123L)
                    .score(85)
                    .drivingTime(12)
                    .lastScore(ScoreInfo.builder().carbon(40).safety(40).accident(40).focus(40).build())
                    .currentScore(ScoreInfo.builder().carbon(60).safety(60).accident(60).focus(60).build())
                    .build();

            doNothing().when(rewardService).calculateAndEarn(any());

            mockMvc.perform(post("/reward/earn")
                            .with(csrf())
                            .header("X-USER-ID", "1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNoContent());
        }
    }
    @WithMockUser(username = "testUser")
    @Test
    @DisplayName("GET /reward/users/{userId}/balance - 성공")
    void getBalanceSuccess() throws Exception {
        Long userId = 1L;
        given(rewardService.getBalance(userId)).willReturn(300L);

        mockMvc.perform(get("/reward/users/{userId}/balance", userId)
                        .header("X-USER-ID", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(userId))
                .andExpect(jsonPath("$.data.balance").value(300));
    }

    @WithMockUser
    @Test
    @DisplayName("GET /reward/users/{userId}/history - 성공")
    void getHistorySuccess() throws Exception {
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        Reward reward = Reward.builder()
                .userId(userId)
                .amount(100L)
                .description("테스트 보상")
                .type(RewardType.EARNED)
                .balanceSnapshot(300L)
                .build();
        Page<Reward> page = new PageImpl<>(List.of(reward), pageable, 1);

        when(rewardService.getRewardHistory(userId, pageable)).thenReturn(page);

        mockMvc.perform(get("/reward/users/{userId}/history", userId)
                        .param("page", "0").param("size", "10")
                        .header("X-USER-ID", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rewardHistory[0].userId").value(userId))
                .andExpect(jsonPath("$.data.rewardHistory[0].description").value("테스트 보상"));
    }
}
