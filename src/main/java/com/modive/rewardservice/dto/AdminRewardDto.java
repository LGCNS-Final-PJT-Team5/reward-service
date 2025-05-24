package com.modive.rewardservice.dto;

import com.modive.rewardservice.domain.Reward;
import com.modive.rewardservice.domain.RewardReason;
import lombok.*;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

    // MonthlyIssued 관련 DTO (오타 수정: montlyIssued -> monthlyIssued)
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

    // TotalReasonStats 관련 DTO
    @Getter
    @Builder
    public static class TotalReasonStatsResponse {
        private List<ReasonStat> totalRewardStatistics;

        public static TotalReasonStatsResponse of(List<ReasonStat> stats) {
            return TotalReasonStatsResponse.builder()
                    .totalRewardStatistics(stats)
                    .build();
        }

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
    }

    // MonthlyStats 관련 DTO (오타 수정: monthlyRewardStatics -> monthlyRewardStatistics)
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
        private Long driveId;
        private int rewards;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RewardsByDriveRequest {
        private List<Long> driveIds;
    }

    // RewardFilter 관련 DTO
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
    }

    @Getter
    @Builder
    public static class FilteredReward {
        private String rewardId;
        private String email;
        private LocalDateTime createdAt;
        private String description;
        private int amount;
    }
}