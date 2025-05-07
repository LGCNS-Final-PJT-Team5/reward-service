package com.modive.rewardservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.modive.rewardservice.entity.enums.RewardReason;
import com.modive.rewardservice.entity.enums.RewardType;
import com.modive.rewardservice.dto.RewardEarnRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class RewardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void earnRewardTest() throws Exception {
        RewardEarnRequest request = new RewardEarnRequest();
        request.setUserId("testUser1");
        request.setAmount(10);
        request.setType(RewardType.EARN);
        request.setReason(RewardReason.MOBTI_CHANGE);
        request.setDescription("MBTI 변경 보상");

        mockMvc.perform(post("/reward/earn")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value("testUser1"))
                .andExpect(jsonPath("$.data.amount").value(10));
    }
}
