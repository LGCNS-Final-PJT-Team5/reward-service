package com.modive.rewardservice.exception;

public class InvalidPayloadException extends RuntimeException {
  public InvalidPayloadException(String fieldName) {
    super("필수 필드 '" + fieldName + "'가 누락되었습니다.");
  }
}