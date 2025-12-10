package com.jibangyoung.global.common;

import lombok.Getter;

/**
 * 🌐 모든 API 응답의 공통 Wrapper
 */
@Getter
public class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final String message;
    private final String errorCode;

    // ✅ 명시적 생성자: 모든 필드 포함
    public ApiResponse(boolean success, T data, String message, String errorCode) {
        this.success = success;
        this.data = data;
        this.message = message;
        this.errorCode = errorCode;
    }

    // ✅ 성공 응답 (데이터만)
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, null);
    }

    // ✅ 성공 응답 (데이터 + 메시지)
    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, data, message, null);
    }

    // ✅ 실패 응답 (에러 메시지 + 에러 코드)
    public static <T> ApiResponse<T> error(String message, String errorCode) {
        return new ApiResponse<>(false, null, message, errorCode);
    }

    // ✅ 실패 응답 (에러 메시지만)
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, null, message, null);
    }
}