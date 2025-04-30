package com.modive.rewardservice.entity;

import lombok.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Reward")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reward {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userId;

    private Integer amount; // 씨앗 양 (양수: 적립, 음수: 사용)

    private String type; // "EARN" 또는 "USE"

    private String reason; // "DRIVING_RECORD", "EVENT", "PURCHASE" 등

    private String description; // "주행 점수 보상", "주행 이벤트 보상", "할인 쿠폰 구매" 등

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}