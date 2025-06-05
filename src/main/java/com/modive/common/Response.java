package com.modive.common;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Response<T> {
    private int status;
    private String message;
    private T data;

    public static <T> Response<T> success(int status, String message, T data) {
        return Response.<T>builder()
                .status(status)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> Response<T> error(int status, String message) {
        return Response.<T>builder()
                .status(status)
                .message(message)
                .data(null)
                .build();
    }
}