package com.modive.rewardservice.controller;


import com.modive.rewardservice.config.UserIdInterceptor;
import com.modive.rewardservice.domain.Reward;
import com.modive.rewardservice.dto.*;
import com.modive.rewardservice.service.RewardService;
import com.modive.common.Response;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;



@RestController
@RequestMapping("/reward")
@RequiredArgsConstructor
public class RewardController {

    private final RewardService rewardService;

    @PostMapping("/earn")
    public ResponseEntity<Void> earnComplexRewards(
            @Valid @RequestBody RewardDto.EarnComplexRequest request) {
        String userId = UserIdInterceptor.getCurrentUserId();
        rewardService.calculateAndEarn(request.toServiceRequest(userId));
        return ResponseEntity.noContent().build();
    }


    @GetMapping("/users/balance")
    public ResponseEntity<Response<RewardDto.BalanceResponse>> getBalance() {
        String userId = UserIdInterceptor.getCurrentUserId();
        Long balance = rewardService.getBalance(userId);
        RewardDto.BalanceResponse data = RewardDto.BalanceResponse.of(userId, balance);
        return ResponseEntity.ok(Response.success(200, "씨앗 잔액 조회에 성공하였습니다.", data));
    }

    @GetMapping("/users/history")
    public ResponseEntity<RewardDto.HistoryResponse> getRewardHistory(Pageable pageable) {
        String userId = UserIdInterceptor.getCurrentUserId();
        Page<Reward> page = rewardService.getRewardHistory(userId, pageable);
        return ResponseEntity.ok(RewardDto.HistoryResponse.of(page));
    }
}
