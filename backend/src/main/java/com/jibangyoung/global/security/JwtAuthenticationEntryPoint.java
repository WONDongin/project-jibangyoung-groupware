package com.jibangyoung.global.security;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException, ServletException {

        String requestURI = request.getRequestURI();
        String method = request.getMethod();

        log.warn("[JWT_ENTRY_POINT] 인증 실패 - {} {} - {}", method, requestURI, authException.getMessage());

        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        // ✅ 토큰 만료 상황을 더 명확하게 구분
        String errorCode = determineErrorCode(request, authException);
        String errorMessage = determineErrorMessage(errorCode);

        Map<String, Object> body = new HashMap<>();
        body.put("status", HttpServletResponse.SC_UNAUTHORIZED);
        body.put("error", "Unauthorized");
        body.put("code", errorCode);
        body.put("message", errorMessage);
        body.put("path", requestURI);
        body.put("timestamp", System.currentTimeMillis());

        // ✅ 추가 디버깅 정보 (개발 환경에서만)
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null) {
            body.put("hasAuthHeader", true);
            body.put("authHeaderType", authHeader.startsWith("Bearer ") ? "Bearer" : "Other");
        } else {
            body.put("hasAuthHeader", false);
        }

        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(response.getOutputStream(), body);
    }

    /**
     * ✅ 에러 코드 결정 로직
     */
    private String determineErrorCode(HttpServletRequest request, AuthenticationException authException) {
        String authHeader = request.getHeader("Authorization");

        // Authorization 헤더가 없는 경우
        if (authHeader == null || authHeader.trim().isEmpty()) {
            return "MISSING_TOKEN";
        }

        // Bearer 형식이 아닌 경우
        if (!authHeader.startsWith("Bearer ")) {
            return "INVALID_TOKEN_FORMAT";
        }

        // 토큰이 있지만 인증 실패한 경우 (만료 또는 무효)
        String token = authHeader.substring(7).trim();
        if (token.isEmpty()) {
            return "EMPTY_TOKEN";
        }

        // 일반적인 토큰 만료/무효 상황
        return "TOKEN_EXPIRED";
    }

    /**
     * ✅ 에러 메시지 결정 로직
     */
    private String determineErrorMessage(String errorCode) {
        switch (errorCode) {
            case "MISSING_TOKEN":
                return "인증 토큰이 필요합니다.";
            case "INVALID_TOKEN_FORMAT":
                return "토큰 형식이 올바르지 않습니다.";
            case "EMPTY_TOKEN":
                return "토큰이 비어있습니다.";
            case "TOKEN_EXPIRED":
                return "토큰이 만료되었거나 유효하지 않습니다. 다시 로그인해주세요.";
            default:
                return "인증이 필요합니다.";
        }
    }
}