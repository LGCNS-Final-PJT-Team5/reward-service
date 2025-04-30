package com.modive.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Response<T> {
    private boolean success;
    private String message;
    private T data;

    public static <T> Response<T> success(T data) {
        return Response.<T>builder()
                .success(true)
                .message("성공")
                .data(data)
                .build();
    }

    public static <T> Response<T> error(String message) {
        return Response.<T>builder()
                .success(false)
                .message(message)
                .data(null)
                .build();
    }
}
