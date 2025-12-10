package com.jibangyoung.domain.auth.controller;

import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jibangyoung.domain.auth.dto.SocialLoginStatusDto;
import com.jibangyoung.domain.auth.dto.UserInfoDto;
import com.jibangyoung.domain.auth.entity.User;
import com.jibangyoung.domain.auth.service.UserService;
import com.jibangyoung.global.annotation.UserActivityLogging;
import com.jibangyoung.global.common.ApiResponse;
import com.jibangyoung.global.exception.BusinessException;
import com.jibangyoung.global.exception.ErrorCode;
import com.jibangyoung.global.security.JwtTokenProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController("authMeController")
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthMeController {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;

    @GetMapping("/me")
    @UserActivityLogging(actionType = "USER_INFO_VIEW", priority = UserActivityLogging.Priority.NORMAL, description = "현재 사용자 정보 조회")
    public ApiResponse<UserInfoDto> getCurrentUser(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String token = extractBearerToken(authHeader);
        validateTokenOrThrow(token);

        String username = jwtTokenProvider.getAuthentication(token).getName();
        User user = userService.getUserEntityByUsername(username);
        return ApiResponse.success(UserInfoDto.from(user), "사용자 정보 조회 성공");
    }

    @GetMapping("/social/status")
    @UserActivityLogging(actionType = "SOCIAL_STATUS_VIEW", priority = UserActivityLogging.Priority.NORMAL, description = "소셜 로그인 상태 조회")
    public ApiResponse<SocialLoginStatusDto> getSocialLoginStatus(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String token = extractBearerToken(authHeader);
        validateTokenOrThrow(token);

        String username = jwtTokenProvider.getAuthentication(token).getName();
        boolean isNaverUser = username != null && username.startsWith("naver_");

        SocialLoginStatusDto status = SocialLoginStatusDto.builder()
                .isSocialLogin(isNaverUser)
                .provider(isNaverUser ? "naver" : null)
                .canChangePassword(!isNaverUser)
                .build();

        return ApiResponse.success(status, "소셜 로그인 상태 조회 성공");
    }

    private String extractBearerToken(String authHeader) {
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            throw new BusinessException(ErrorCode.INVALID_LOGIN_CREDENTIALS, "토큰이 필요합니다.");
        }
        return authHeader.substring("Bearer ".length());
    }

    private void validateTokenOrThrow(String token) {
        try {
            if (!StringUtils.hasText(token) || !jwtTokenProvider.validateToken(token)) {
                throw new BusinessException(ErrorCode.INVALID_LOGIN_CREDENTIALS, "유효하지 않은 토큰입니다.");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[/api/auth/* manual check] 토큰 검증 오류: {}", e.getMessage());
            throw new BusinessException(ErrorCode.INVALID_LOGIN_CREDENTIALS, "토큰 검증 중 오류가 발생했습니다.");
        }
    }
}