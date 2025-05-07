package com.modive.rewardservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MonthlyRewardStatsDTO {
    private String month;   // ex) "2025-05"
    private int total;      // 발급량 합계
}