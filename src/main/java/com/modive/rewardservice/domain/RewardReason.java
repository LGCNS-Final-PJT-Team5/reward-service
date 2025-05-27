package com.modive.rewardservice.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum RewardReason {
    TOTAL_SCORE("종합점수"),
    EVENT_NOT_OCCURRED("이벤트미발생"),
    MOBTI_IMPROVEMENT("MoBTI향상"),
    UNKNOWN("알 수 없음");

    private final String label;

    public static RewardReason fromDescription(String description) {
        return Arrays.stream(values())
                .filter(r -> r.getLabel().equals(description))
                .findFirst()
                .orElse(UNKNOWN);
    }
    public String getLabel() {
        return label;
    }
}
