package com.modive.rewardservice.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RewardReason {

    TOTAL_SCORE("종합점수"),
    EVENT_NOT_OCCURRED("주행 중 (10분)"),
    ATTENDANCE("출석점수"),
    LOYALTY("장기고객 리워드"),
    MOBTI_IMPROVED("MoBTI 향상");

    private final String label;

    public static RewardReason from(String label) {
        for (RewardReason reason : RewardReason.values()) {
            if (reason.getLabel().equals(label)) {
                return reason;
            }
        }
        throw new IllegalArgumentException("Unknown reward reason label: " + label);
    }
}
