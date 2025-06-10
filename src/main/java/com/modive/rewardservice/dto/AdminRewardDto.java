package com.modive.rewardservice.dto;

import com.modive.rewardservice.domain.Reward;
import com.modive.rewardservice.domain.RewardReason;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

public class AdminRewardDto {

    // TotalIssued 관련 DTO
    @Getter
    @Builder
    public static class TotalIssuedResponse {
        private TotalIssued totalIssued;

        public static TotalIssuedResponse of(long value, double changeRate) {
            return TotalIssuedResponse.builder()
                    .totalIssued(TotalIssued.of(value, changeRate))
                    .build();
        }
    }

    @Getter
    @Builder
    public static class TotalIssued {
        private long value;
        private double changeRate;

        public static TotalIssued of(long value, double changeRate) {
            return TotalIssued.builder()
                    .value(value)
                    .changeRate(changeRate)
                    .build();
        }
    }

    // MonthlyIssued 관련 DTO
    @Getter
    @Builder
    public static class MonthlyIssuedResponse {
        private MonthlyIssued monthlyIssued;

        public static MonthlyIssuedResponse of(long value, double changeRate) {
            return MonthlyIssuedResponse.builder()
                    .monthlyIssued(MonthlyIssued.of(value, changeRate))
                    .build();
        }
    }

    @Getter
    @Builder
    public static class MonthlyIssued {
        private long value;
        private double changeRate;

        public static MonthlyIssued of(long value, double changeRate) {
            return MonthlyIssued.builder()
                    .value(value)
                    .changeRate(changeRate)
                    .build();
        }
    }

    // DailyAverageIssued 관련 DTO
    @Getter
    @Builder
    public static class DailyAverageIssuedResponse {
        private DailyAverageIssued dailyAverageIssued;

        public static DailyAverageIssuedResponse of(double value, double changeRate) {
            return DailyAverageIssuedResponse.builder()
                    .dailyAverageIssued(DailyAverageIssued.of(value, changeRate))
                    .build();
        }
    }

    @Getter
    @Builder
    public static class DailyAverageIssued {
        private double value;
        private double changeRate;

        public static DailyAverageIssued of(double value, double changeRate) {
            return DailyAverageIssued.builder()
                    .value(value)
                    .changeRate(changeRate)
                    .build();
        }
    }

    // PerUserAverageIssued 관련 DTO
    @Getter
    @Builder
    public static class PerUserAverageIssuedResponse {
        private PerUserAverageIssued perUserAverageIssued;

        public static PerUserAverageIssuedResponse of(double value, double changeRate) {
            return PerUserAverageIssuedResponse.builder()
                    .perUserAverageIssued(PerUserAverageIssued.of(value, changeRate))
                    .build();
        }
    }

    @Getter
    @Builder
    public static class PerUserAverageIssued {
        private double value;
        private double changeRate;

        public static PerUserAverageIssued of(double value, double changeRate) {
            return PerUserAverageIssued.builder()
                    .value(value)
                    .changeRate(changeRate)
                    .build();
        }
    }

    // 발급 사유별 총 통계 & 월 통계 DTO
    @Getter
    @Builder
    public static class ReasonStat {
        private String reason;
        private long count;
        private double ratio;

        public static ReasonStat of(RewardReason reasonEnum, long count, double ratio) {
            return ReasonStat.builder()
                    .reason(reasonEnum.getLabel())
                    .count(count)
                    .ratio(ratio)
                    .build();
        }
    }

    @Getter
    @Builder
    public static class TotalReasonStatsResponse {
        private List<ReasonStat> totalRewardStatistics;

        public static TotalReasonStatsResponse of(List<ReasonStat> stats) {
            return TotalReasonStatsResponse.builder().totalRewardStatistics(stats).build();
        }
    }

    @Getter
    @Builder
    public static class MonthlyReasonStatsResponse {
        private List<ReasonStat> monthlyRewardStatistics;

        public static MonthlyReasonStatsResponse of(List<ReasonStat> stats) {
            return MonthlyReasonStatsResponse.builder().monthlyRewardStatistics(stats).build();
        }
    }

    // MonthlyStats 관련 DTO
    @Getter
    @Builder
    public static class MonthlyStatsResponse {
        private List<MonthlyRewardStat> monthlyRewardStatistics;

        public static MonthlyStatsResponse of(List<MonthlyRewardStat> stats) {
            return MonthlyStatsResponse.builder()
                    .monthlyRewardStatistics(stats)
                    .build();
        }
    }

    @Getter
    @Builder
    @Setter
    public static class MonthlyRewardStat {
        private int year;
        private int month;
        private int amount;
    }

    // AllRewardHistory 관련 DTO
    @Getter
    @Builder
    public static class AllRewardHistoryResponse {
        private List<RewardHistoryItem> rewardHistory;
        private PageInfoDTO pageInfo;

        public static AllRewardHistoryResponse of(Page<RewardHistoryItem> page) {
            return AllRewardHistoryResponse.builder()
                    .rewardHistory(page.getContent())
                    .pageInfo(new PageInfoDTO(
                            page.getNumber() + 1,
                            page.getSize(),
                            page.getTotalElements(),
                            page.getTotalPages()
                    ))
                    .build();
        }

        @Getter
        @Builder
        public static class RewardHistoryItem {
            private String rewardId;
            private String userId;
            private LocalDate issuedDate;
            private String reason;
            private int amount;

            public static RewardHistoryItem from(Reward reward) {
                return RewardHistoryItem.builder()
                        .rewardId("SEED_" + reward.getId())
                        .issuedDate(reward.getCreatedAt().toLocalDate())
                        .reason(reward.getDescription())
                        .amount(reward.getAmount().intValue())
                        .build();
            }
        }
    }

    // PageInfoDTO
    @Getter
    @AllArgsConstructor
    public static class PageInfoDTO {
        private int currentPage;
        private int pageSize;
        private long totalElements;
        private int totalPages;
    }

    // RewardsByDrive 관련 DTO
    @Getter
    @Builder
    public static class RewardsByDriveResponse {
        private List<DriveReward> rewardsByDrive;

        public static RewardsByDriveResponse of(List<DriveReward> rewards) {
            return RewardsByDriveResponse.builder()
                    .rewardsByDrive(rewards)
                    .build();
        }
    }

    @Getter
    @Builder
    public static class DriveReward {
        private String driveId;
        private int rewards;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RewardsByDriveRequest {
        @NotEmpty(message = "Drive IDs cannot be empty")
        private List<String> driveIds;
    }

    // 🔧 개선: 이메일 입력, userId 출력 방식의 검색 요청 DTO
    @Getter
    @Setter  // 🔧 추가: Setter for flexibility
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RewardSearchRequest {
        @Email(message = "올바른 이메일 형식이 아닙니다")
        private String email; // 🔧 입력용: 관리자가 이메일로 검색

        @Size(max = 100, message = "설명은 100자를 초과할 수 없습니다")
        private String description;

        private LocalDate startDate;
        private LocalDate endDate;

        private List<String> reasons; // 다중 사유 선택 가능

        @Min(value = 0, message = "최소 금액은 0 이상이어야 합니다")
        private Long minAmount;

        @Min(value = 0, message = "최대 금액은 0 이상이어야 합니다")
        private Long maxAmount;

        // 🔧 검증 로직
        public void validate() {
            if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
                throw new IllegalArgumentException("시작일은 종료일보다 이전이어야 합니다.");
            }
            if (minAmount != null && maxAmount != null && minAmount > maxAmount) {
                throw new IllegalArgumentException("최소 금액은 최대 금액보다 작아야 합니다.");
            }
        }
    }

    // 🔧 개선: userId 기반 응답 DTO
    @Getter
    @Builder
    public static class RewardFilterResponse {
        private List<FilteredReward> searchResult;
        private PageInfoDTO pageInfo;

        public static RewardFilterResponse of(List<FilteredReward> rewards, Page<?> page) {
            return RewardFilterResponse.builder()
                    .searchResult(rewards)
                    .pageInfo(new PageInfoDTO(
                            page.getNumber() + 1,
                            page.getSize(),
                            page.getTotalElements(),
                            page.getTotalPages()
                    ))
                    .build();
        }

        // 🔧 빈 결과 반환용 (사용자를 못 찾았을 때)
        public static RewardFilterResponse empty() {
            return RewardFilterResponse.builder()
                    .searchResult(Collections.emptyList())
                    .pageInfo(new PageInfoDTO(1, 0, 0, 0))
                    .build();
        }
    }

    // 🔧 개선: userId 출력 방식의 필터링된 리워드 DTO
    @Getter
    @Builder
    public static class FilteredReward {
        private String rewardId;
        private String userId; // 🔧 핵심: email → userId로 변경하여 MSA 경계 명확화
        private LocalDateTime issuedDate;
        private String reason;
        private int amount;

        // 🔧 추가: 팩토리 메서드로 생성 간소화
        public static FilteredReward from(Reward reward, RewardReason reasonEnum) {
            return FilteredReward.builder()
                    .rewardId("SEED_" + reward.getId())
                    .userId(reward.getUserId().toString())
                    .issuedDate(reward.getCreatedAt())
                    .reason(reasonEnum.getLabel())
                    .amount(reward.getAmount().intValue())
                    .build();
        }
    }

    // 에러 응답 DTO
    @Getter
    @Builder
    public static class ErrorResponse {
        private String message;
        private String code;
        private LocalDateTime timestamp;

        public static ErrorResponse of(String message, String code) {
            return ErrorResponse.builder()
                    .message(message)
                    .code(code)
                    .timestamp(LocalDateTime.now())
                    .build();
        }
    }

    // 통계 요약 DTO
    @Getter
    @Builder
    public static class RewardSummaryResponse {
        private long totalRewards;
        private long totalUsers;
        private double averagePerUser;
        private LocalDate lastUpdated;

        public static RewardSummaryResponse of(long totalRewards, long totalUsers, LocalDate lastUpdated) {
            double average = totalUsers > 0 ? (double) totalRewards / totalUsers : 0.0;
            return RewardSummaryResponse.builder()
                    .totalRewards(totalRewards)
                    .totalUsers(totalUsers)
                    .averagePerUser(Math.round(average * 100) / 100.0)
                    .lastUpdated(lastUpdated)
                    .build();
        }
    }
}