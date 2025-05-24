package com.modive.rewardservice.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import jakarta.persistence.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;

@Entity
@Table(name = "rewards")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Reward {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RewardType type;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private Long balanceSnapshot;

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reward_balance_id")
    private RewardBalance rewardBalance;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = true)
    private Long driveId;

    @Builder
    public Reward(Long userId, Long amount, RewardType type, String description, Long balanceSnapshot, RewardBalance rewardBalance, Long driveId) {
        this.userId = userId;
        this.amount = amount;
        this.type = type;
        this.description = description;
        this.balanceSnapshot = balanceSnapshot;
        this.rewardBalance = rewardBalance;
        this.driveId = driveId;
        this.version = 0L;
    }
}