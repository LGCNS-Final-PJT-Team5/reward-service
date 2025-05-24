package com.modive.rewardservice.exception;

public class InternalServerErrorException extends RuntimeException {
    public InternalServerErrorException() {
        super("예기치 않은 오류가 발생했습니다. 관리자에게 문의하세요.");
    }
}