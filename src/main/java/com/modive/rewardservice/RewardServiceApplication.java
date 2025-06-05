package com.modive.rewardservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@EnableFeignClients(basePackages = "com.modive.rewardservice.client")
@SpringBootApplication
@ComponentScan(basePackages = {"com.modive.rewardservice", "com.modive.common"})
public class RewardServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RewardServiceApplication.class, args);
    }

}
