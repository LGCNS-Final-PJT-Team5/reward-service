package com.modive.rewardservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients(basePackages = "com.modive.rewardservice.client")
@SpringBootApplication
public class RewardServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RewardServiceApplication.class, args);
    }

}
