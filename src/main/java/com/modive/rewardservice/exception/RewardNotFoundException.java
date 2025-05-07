package com.modive.rewardservice.exception;

public class RewardNotFoundException extends RuntimeException {
    public RewardNotFoundException(Long id) {
        super("리워드 ID를 찾을 수 없습니다: " + id);
    }
}
