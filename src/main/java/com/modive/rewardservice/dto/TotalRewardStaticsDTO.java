package com.modive.rewardservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TotalRewardStaticsDTO {
    private String reason;
    private int count;
    private double ratio;
}