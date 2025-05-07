package com.modive.rewardservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PerUserAverageIssuedDTO {
    private long value;
    private double changeRate;
}