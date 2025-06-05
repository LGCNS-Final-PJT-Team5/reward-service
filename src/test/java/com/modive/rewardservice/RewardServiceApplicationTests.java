package com.modive.rewardservice;

import com.modive.rewardservice.config.UserIdInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
class RewardServiceApplicationTests {

    @MockBean
    private UserIdInterceptor userIdInterceptor;

    @Test
    void contextLoads() {
    }

}
