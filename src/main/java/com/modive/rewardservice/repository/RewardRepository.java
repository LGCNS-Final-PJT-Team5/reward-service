package com.modive.rewardservice.repository;

import com.modive.rewardservice.domain.Reward;
import com.modive.rewardservice.domain.RewardType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RewardRepository extends JpaRepository<Reward, Long> {

    @Query("SELECT r FROM Reward r LEFT JOIN FETCH r.rewardBalance WHERE r.userId = :userId ORDER BY r.createdAt DESC")
    Page<Reward> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);

    // 🔧 수정: Enum 직접 사용 - 상수로 하드코딩
    @Query("SELECT COUNT(r) FROM Reward r WHERE r.type = com.modive.rewardservice.domain.RewardType.EARNED")
    long getTotalIssued();

    @Query("SELECT COUNT(r) FROM Reward r WHERE r.type = com.modive.rewardservice.domain.RewardType.EARNED AND r.createdAt < :dateTime")
    long countIssuedBefore(@Param("dateTime") LocalDateTime dateTime);

    @Query("SELECT COUNT(r) FROM Reward r WHERE r.type = com.modive.rewardservice.domain.RewardType.EARNED AND r.createdAt >= :start AND r.createdAt <= :end")
    long countIssuedBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(DISTINCT r.userId) FROM Reward r WHERE r.type = com.modive.rewardservice.domain.RewardType.EARNED AND r.createdAt >= :start AND r.createdAt <= :end")
    long countDistinctUsersBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT r.description, COUNT(r) FROM Reward r WHERE r.type = com.modive.rewardservice.domain.RewardType.EARNED AND YEAR(r.createdAt) = YEAR(CURRENT_DATE) GROUP BY r.description")
    List<Object[]> getCurrentYearIssuedGroupedByReason();

    @Query("SELECT r.description, COUNT(r) " +
            "FROM Reward r " +
            "WHERE FUNCTION('DATE_FORMAT', r.createdAt, '%Y-%m') = :month AND r.type = com.modive.rewardservice.domain.RewardType.EARNED " +
            "GROUP BY r.description")
    List<Object[]> getMonthlyRewardStatsByReason(@Param("month") String month);

    // 🔧 수정: EARNED 조건 추가
    @Query("SELECT YEAR(r.createdAt), MONTH(r.createdAt), SUM(r.amount) FROM Reward r WHERE r.type = com.modive.rewardservice.domain.RewardType.EARNED AND r.createdAt >= :startDate GROUP BY YEAR(r.createdAt), MONTH(r.createdAt) ORDER BY YEAR(r.createdAt), MONTH(r.createdAt)")
    List<Object[]> findMonthlyIssuedStatsLast12Months(@Param("startDate") LocalDateTime startDate);

    @Query("SELECT r FROM Reward r ORDER BY r.createdAt DESC")
    Page<Reward> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // 기존 필터링 쿼리 (type 조건 없음 - 전체 조회용)
    @Query("SELECT r FROM Reward r WHERE (:userId IS NULL OR r.userId = :userId) AND (:description IS NULL OR r.description LIKE %:description%) AND (:startDate IS NULL OR r.createdAt >= :startDate) AND (:endDate IS NULL OR r.createdAt <= :endDate)")
    Page<Reward> filterRewards(
            @Param("userId") Long userId,
            @Param("description") String description,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    // 고급 검색 쿼리 (type 조건 없음 - 전체 조회용)
    @Query("SELECT r FROM Reward r WHERE " +
            "(:userId IS NULL OR r.userId = :userId) AND " +
            "(:description IS NULL OR r.description LIKE %:description%) AND " +
            "(:startDate IS NULL OR r.createdAt >= :startDate) AND " +
            "(:endDate IS NULL OR r.createdAt <= :endDate) AND " +
            "(:reasons IS NULL OR :reasons = '' OR r.description IN :reasons) AND " +
            "(:minAmount IS NULL OR r.amount >= :minAmount) AND " +
            "(:maxAmount IS NULL OR r.amount <= :maxAmount)")
    Page<Reward> searchRewards(
            @Param("userId") Long userId,
            @Param("description") String description,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("reasons") List<String> reasons,
            @Param("minAmount") Long minAmount,
            @Param("maxAmount") Long maxAmount,
            Pageable pageable
    );

    @Query("SELECT SUM(r.amount) FROM Reward r WHERE r.driveId = :driveId")
    Optional<Integer> sumAmountByDriveId(@Param("driveId") Long driveId);

    @Query("SELECT COUNT(r) FROM Reward r " +
            "WHERE r.userId = :userId " +
            "AND r.description LIKE :description " +
            "AND r.createdAt BETWEEN :start AND :end")
    long countByUserIdAndDescriptionLikeAndDateRange(
            @Param("userId") Long userId,
            @Param("description") String description,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}