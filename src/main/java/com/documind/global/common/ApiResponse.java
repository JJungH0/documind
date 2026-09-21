package com.documind.global.common;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        T data,
        ErrorBody error
) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static ApiResponse<Void> error(String code, String message) {
        return new ApiResponse<>(false, null, new ErrorBody(code, message));
    }

    public record ErrorBody(String code, String message) { }

}
