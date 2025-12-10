package com.jibangyoung.global.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "API 표준 응답")
public class ApiResponse<T> {

    @Schema(description = "성공 여부", example = "true")
    private final boolean success;

    @Schema(description = "응답 데이터")
    private final T data;

    @Schema(description = "실패 시 에러코드")
    private final String errorCode;

    @Schema(description = "실패 시 메시지")
    private final String message;

    // 성공 응답
    private ApiResponse(T data) {
        this.success = true;
        this.data = data;
        this.errorCode = null;
        this.message = null;
    }

    // 실패 응답
    private ApiResponse(String errorCode, String message) {
        this.success = false;
        this.data = null;
        this.errorCode = errorCode;
        this.message = message;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(data);
    }
    public static <T> ApiResponse<T> fail(String errorCode, String message) {
        return new ApiResponse<>(errorCode, message);
    }
}
