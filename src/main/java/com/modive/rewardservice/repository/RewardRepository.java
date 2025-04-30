package com.modive.rewardservice.repository;

import com.modive.rewardservice.entity.Reward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RewardRepository extends JpaRepository<Reward, Long> {

    List<Reward> findByUserIdOrderByCreatedAtDesc(String userId);

    @Query("SELECT SUM(r.amount) FROM Reward r WHERE r.userId = :userId AND r.amount > 0")
    Integer getTotalEarnedByUserId(String userId);

    @Query("SELECT SUM(ABS(r.amount)) FROM Reward r WHERE r.userId = :userId AND r.amount < 0")
    Integer getTotalUsedByUserId(String userId);

    @Query("SELECT SUM(r.amount) FROM Reward r WHERE r.userId = :userId")
    Integer getCurrentBalanceByUserId(String userId);
}