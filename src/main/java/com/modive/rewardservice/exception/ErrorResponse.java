package com.modive.rewardservice.exception;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ErrorResponse {
  private int status;
  private String message;
  private ErrorDetail error;

  @Data
  @AllArgsConstructor
  public static class ErrorDetail {
    private String code;
    private String details;
  }
}
