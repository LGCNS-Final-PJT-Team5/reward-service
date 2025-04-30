package com.modive.rewardservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.modive.rewardservice.entity.Reward;
import com.modive.rewardservice.repository.RewardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class RewardControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private RewardRepository rewardRepository;

    private RestTemplate restTemplate;

    private final String userId = "integrationUser";

    @BeforeEach
    void setup() {
        restTemplate = new RestTemplate();
        rewardRepository.deleteAll();
    }

    @Test
    @DisplayName("[통합] 씨앗 적립 및 요약 정보 확인")
    void testEarnAndSummary() {
        String earnUrl = "http://localhost:" + port + "/reward/{userId}/earn?amount=10&reason=DRIVING_RECORD&description=주행테스트";

        Map<String, String> uriVars = new HashMap<>();
        uriVars.put("userId", userId);

        ResponseEntity<String> earnResponse = restTemplate.getForEntity(earnUrl, String.class, uriVars);
        assertThat(earnResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        String summaryUrl = "http://localhost:" + port + "/reward/{userId}";
        ResponseEntity<String> summaryResponse = restTemplate.getForEntity(summaryUrl, String.class, uriVars);
        assertThat(summaryResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(summaryResponse.getBody()).contains("availableSeeds");
        assertThat(summaryResponse.getBody()).contains("history");
    }

    @Test
    @DisplayName("[통합] 씨앗 사용 및 총 사용 씨앗 확인")
    void testUseAndTotalUsed() {
        // 적립 먼저 수행
        rewardRepository.save(Reward.builder()
                .userId(userId)
                .amount(50)
                .type("EARN")
                .reason("DRIVING")
                .description("초기 적립")
                .build());

        String useUrl = "http://localhost:" + port + "/reward/{userId}/use?amount=20&reason=PURCHASE&description=쿠폰 사용";
        Map<String, String> uriVars = new HashMap<>();
        uriVars.put("userId", userId);

        ResponseEntity<String> useResponse = restTemplate.getForEntity(useUrl, String.class, uriVars);
        assertThat(useResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        String summaryUrl = "http://localhost:" + port + "/reward/{userId}";
        ResponseEntity<String> summaryResponse = restTemplate.getForEntity(summaryUrl, String.class, uriVars);
        assertThat(summaryResponse.getBody()).contains("totalUsedSeeds");
        assertThat(summaryResponse.getBody()).contains("20");
    }
}
