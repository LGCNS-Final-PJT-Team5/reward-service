package com.modive.rewardservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SearchRewardResultDTO {
    private String rewardId; // ex) "SEED_1024"
    private String email;
}