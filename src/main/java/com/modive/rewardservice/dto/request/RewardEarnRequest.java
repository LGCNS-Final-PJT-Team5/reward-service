package com.modive.rewardservice.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.modive.rewardservice.dto.*;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RewardEarnRequest {
    private String userId;
    private String driveId;
    private Integer score;
    private Integer drivingTime;

    private ScoreInfo lastScore;
    private ScoreInfo currentScore;
}