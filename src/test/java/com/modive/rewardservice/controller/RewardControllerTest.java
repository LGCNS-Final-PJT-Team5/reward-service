
package com.modive.rewardservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.modive.rewardservice.config.UserIdInterceptor;
import com.modive.rewardservice.dto.RewardDto;
import com.modive.rewardservice.dto.request.ScoreInfo;
import com.modive.rewardservice.service.RewardService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import org.springframework.security.test.context.support.WithMockUser;

@WebMvcTest(RewardController.class)
class RewardControllerTest {

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
                    .driveId("1")
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
}
