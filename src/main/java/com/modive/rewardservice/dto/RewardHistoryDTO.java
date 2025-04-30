package com.modive.rewardservice.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RewardHistoryDTO {
    private Long id;
    private String description; // 표시될 내용 (예: "주행 점수 보상", "할인 쿠폰 구매")
    private String type; // "적립" 또는 "사용"
    private Integer amount; // 씨앗 양
    private Integer balance; // 잔액
    private LocalDateTime date;
}