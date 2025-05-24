package com.modive.rewardservice.repository;

import com.modive.rewardservice.domain.RewardBalance;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface RewardBalanceRepository extends JpaRepository<RewardBalance, Long> {
    @Lock(LockModeType.OPTIMISTIC)
    Optional<RewardBalance> findByUserId(Long userId);
}
