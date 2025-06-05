package com.modive.rewardservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidPayloadException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPayload(InvalidPayloadException ex) {
        ErrorResponse response = new ErrorResponse(
                400,
                "요청 형식이 잘못되었습니다.",
                new ErrorResponse.ErrorDetail("INVALID_PAYLOAD", ex.getMessage())
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(InternalServerErrorException.class)
    public ResponseEntity<ErrorResponse> handleInternalError(InternalServerErrorException ex) {
        ErrorResponse response = new ErrorResponse(
                500,
                "서버 오류로 인해 대시보드 생성에 실패했습니다.",
                new ErrorResponse.ErrorDetail("INTERNAL_ERROR", ex.getMessage())
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    // 기타 예외 처리도 여기에 추가 가능
}
