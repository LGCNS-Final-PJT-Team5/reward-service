package com.modive.rewardservice.dto;

import com.modive.rewardservice.entity.enums.RewardReason;
import com.modive.rewardservice.entity.enums.RewardType;
import lombok.Data;

@Data
public class RewardEarnRequest {
    private String userId;
    private Integer amount;
    private RewardType type;       // ✅ enum으로 변경
    private RewardReason reason;   // ✅ enum으로 변경
    private String description;
}
