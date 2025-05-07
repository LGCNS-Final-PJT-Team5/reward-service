package com.modive.rewardservice.entity;

import com.modive.rewardservice.entity.enums.RewardReason;
import com.modive.rewardservice.entity.enums.RewardType;
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

    @Enumerated(EnumType.STRING)
    private RewardType type;  // 🔄 enum으로 변경

    @Enumerated(EnumType.STRING)
    private RewardReason reason;  // 🔄 enum으로 변경

    private String description; // "주행 점수 보상", "주행 이벤트 보상", "할인 쿠폰 구매" 등

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}