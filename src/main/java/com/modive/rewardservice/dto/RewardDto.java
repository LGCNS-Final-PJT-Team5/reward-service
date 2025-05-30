package com.modive.rewardservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.modive.rewardservice.domain.Reward;
import com.modive.rewardservice.domain.RewardType;
import com.modive.rewardservice.dto.request.RewardEarnRequest;
import com.modive.rewardservice.dto.request.ScoreInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class RewardDto {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EarnComplexRequest {
        private String driveId;
        private Integer score;

        @JsonProperty("주행 시간")
        private Integer drivingTime;

        private ScoreInfo lastScore;

        private ScoreInfo currentScore;

        public RewardEarnRequest toServiceRequest(String userId) {
            return RewardEarnRequest.builder()
                    .userId(userId)
                    .driveId(driveId)
                    .score(score)
                    .drivingTime(drivingTime)
                    .lastScore(lastScore)
                    .currentScore(currentScore)
                    .build();
        }
    }


    @Getter
    @Builder
    public static class Response {
        private Long id;
        private String userId;
        private Long amount;
        private RewardType type;
        private String description;
        private Long balanceSnapshot;
        private LocalDateTime createdAt;
        private String driveId;

        public static Response from(Reward reward) {
            return Response.builder()
                    .id(reward.getId())
                    .userId(reward.getUserId())
                    .driveId(reward.getDriveId())
                    .amount(reward.getAmount())
                    .type(reward.getType())
                    .description(reward.getDescription())
                    .balanceSnapshot(reward.getBalanceSnapshot())
                    .createdAt(reward.getCreatedAt())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class BalanceResponse {
        private String userId;
        private Long balance;

        public static BalanceResponse of(String userId, Long balance) {
            return BalanceResponse.builder()
                    .userId(userId)
                    .balance(balance)
                    .build();
        }
    }


    @Getter
    @Builder
    public static class HistoryResponse {
        private int status;
        private String message;
        private HistoryData data;

        public static HistoryResponse of(Page<Reward> page) {
            List<Response> rewards = page.getContent().stream()
                    .map(RewardDto.Response::from)
                    .collect(Collectors.toList());

            PageInfoDTO pageInfo = new PageInfoDTO(
                    page.getNumber() + 1,
                    page.getSize(),
                    page.getTotalElements(),
                    page.getTotalPages()
            );

            return HistoryResponse.builder()
                    .status(200)
                    .message("리워드 내역 조회에 성공했습니다.")
                    .data(new HistoryData(rewards, pageInfo))
                    .build();
        }

        @Getter
        @AllArgsConstructor
        public static class HistoryData {
            private List<RewardDto.Response> rewardHistory;
            private PageInfoDTO pageInfo;
        }
    }
}
