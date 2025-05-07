package com.modive.rewardservice.dto;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RewardHistorySimpleDTO {
    private String rewardId; // ex) "SEED_1024"
    private String email;
}