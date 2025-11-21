package com.reliaquest.api.adapter;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(T data, String status, String error) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(data, "Successfully processed request.", null);
    }

    public static <T> ApiResponse<T> error(String error) {
        return new ApiResponse<>(null, "Failed to process request.", error);
    }
}
