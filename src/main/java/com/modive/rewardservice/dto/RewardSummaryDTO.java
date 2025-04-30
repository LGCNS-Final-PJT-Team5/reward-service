package com.modive.rewardservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RewardSummaryDTO {
    private Integer availableSeeds; // 사용 가능 씨앗
    private Integer totalUsedSeeds; // 총 사용 씨앗
    private List<RewardHistoryDTO> history; // 씨앗 적립/사용 내역
}