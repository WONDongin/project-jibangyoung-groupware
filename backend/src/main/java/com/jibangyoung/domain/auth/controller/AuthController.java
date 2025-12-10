package com.jibangyoung.domain.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jibangyoung.domain.auth.dto.CheckEmailResponse;
import com.jibangyoung.domain.auth.dto.CheckUsernameResponse;
import com.jibangyoung.domain.auth.dto.EmailSendRequest;
import com.jibangyoung.domain.auth.dto.EmailVerifyRequest;
import com.jibangyoung.domain.auth.dto.FindIdRequest;
import com.jibangyoung.domain.auth.dto.FindIdResponse;
import com.jibangyoung.domain.auth.dto.LoginRequestDto;
import com.jibangyoung.domain.auth.dto.LoginResponseDto;
import com.jibangyoung.domain.auth.dto.SignupRequestDto;
import com.jibangyoung.domain.auth.dto.UserDto;
import com.jibangyoung.domain.auth.service.AuthService;
import com.jibangyoung.global.annotation.UserActivityLogging;
import com.jibangyoung.global.common.ApiResponse;
import com.jibangyoung.global.exception.BusinessException;
import com.jibangyoung.global.exception.ErrorCode;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Validated
@Slf4j
public class AuthController {

    private final AuthService authService;

    // ======== 회원가입 & 로그인 ========

    @Operation(summary = "회원가입")
    @PostMapping("/signup")
    @UserActivityLogging(actionType = "USER_SIGNUP", scoreDelta = 50, priority = UserActivityLogging.Priority.HIGH, description = "사용자 회원가입")
    public ResponseEntity<ApiResponse<UserDto>> signup(@Valid @RequestBody SignupRequestDto signupRequest) {
        log.info("[SIGNUP] 요청: {}", signupRequest.getUsername());
        UserDto user = authService.signup(signupRequest);
        return ResponseEntity.ok(ApiResponse.success(user, "회원가입이 완료되었습니다."));
    }

    @Operation(summary = "로그인")
    @PostMapping("/login")
    @UserActivityLogging(actionType = "USER_LOGIN", priority = UserActivityLogging.Priority.HIGH, description = "사용자 로그인")
    public ResponseEntity<ApiResponse<LoginResponseDto>> login(@Valid @RequestBody LoginRequestDto loginRequest) {
        log.info("[LOGIN] 요청: {}", loginRequest.getUsername());
        LoginResponseDto loginResponse = authService.login(loginRequest);
        return ResponseEntity.ok(ApiResponse.success(loginResponse, "로그인에 성공했습니다."));
    }

    @Operation(summary = "리프레시 토큰으로 토큰 재발급")
    @PostMapping("/refresh")
    @UserActivityLogging(actionType = "TOKEN_REFRESH", priority = UserActivityLogging.Priority.NORMAL, description = "토큰 재발급")
    public ResponseEntity<ApiResponse<LoginResponseDto>> refreshAccessToken(
            @RequestHeader(value = "Refresh-Token", required = false) String refreshTokenHeader,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        log.info("[REFRESH] 토큰 재발급 요청 수신");

        // ✅ 리프레시 토큰 추출 로직
        String refreshToken = extractRefreshToken(refreshTokenHeader, authHeader);

        if (refreshToken == null) {
            log.warn("[REFRESH] 리프레시 토큰이 제공되지 않음");
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("MISSING_REFRESH_TOKEN", "Refresh-Token 헤더가 필요합니다."));
        }

        // ✅ 토큰 기본 유효성 검사
        if (refreshToken.length() < 20) { // JWT 토큰은 일반적으로 이보다 훨씬 긺
            log.warn("[REFRESH] 토큰 길이가 너무 짧음: {}", refreshToken.length());
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("INVALID_TOKEN_FORMAT", "토큰 형식이 올바르지 않습니다."));
        }

        // ✅ 로깅용 마스킹
        String maskedToken = maskToken(refreshToken);
        log.info("[REFRESH] 토큰 재발급 처리 시작 - token: {}", maskedToken);

        try {
            LoginResponseDto response = authService.refreshToken(refreshToken);

            if (response == null) {
                log.error("[REFRESH] AuthService에서 null 응답 반환");
                return ResponseEntity.status(500)
                        .body(ApiResponse.error("TOKEN_REFRESH_FAILED", "토큰 재발급 처리 중 오류가 발생했습니다."));
            }

            if (response.getAccessToken() == null || response.getAccessToken().trim().isEmpty()) {
                log.error("[REFRESH] 새 액세스 토큰이 비어있음");
                return ResponseEntity.status(500)
                        .body(ApiResponse.error("INVALID_NEW_TOKEN", "새 토큰 생성에 실패했습니다."));
            }

            if (response.getRefreshToken() == null || response.getRefreshToken().trim().isEmpty()) {
                log.error("[REFRESH] 새 리프레시 토큰이 비어있음");
                return ResponseEntity.status(500)
                        .body(ApiResponse.error("INVALID_NEW_REFRESH_TOKEN", "새 리프레시 토큰 생성에 실패했습니다."));
            }

            if (response.getUser() == null) {
                log.error("[REFRESH] 사용자 정보가 비어있음");
                return ResponseEntity.status(500)
                        .body(ApiResponse.error("INVALID_USER_INFO", "사용자 정보 조회에 실패했습니다."));
            }

            log.info("[REFRESH] 토큰 재발급 성공 - user: {}", response.getUser().getUsername());
            return ResponseEntity.ok(ApiResponse.success(response, "토큰이 성공적으로 재발급되었습니다."));

        } catch (BusinessException e) {
            log.error("[REFRESH] 토큰 재발급 실패 - BusinessException: {} ({})",
                    e.getMessage(), e.getErrorCode().getCode());

            return handleRefreshBusinessException(e);

        } catch (IllegalArgumentException e) {
            log.error("[REFRESH] 토큰 재발급 실패 - IllegalArgumentException: {}", e.getMessage());
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("INVALID_TOKEN_FORMAT", "토큰 형식이 올바르지 않습니다."));

        } catch (SecurityException e) {
            log.error("[REFRESH] 토큰 재발급 실패 - SecurityException: {}", e.getMessage());
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("SECURITY_ERROR", "보안 오류가 발생했습니다."));

        } catch (Exception e) {
            log.error("[REFRESH] 토큰 재발급 실패 - 예상치 못한 오류: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("TOKEN_REFRESH_FAILED", "토큰 재발급 중 서버 오류가 발생했습니다."));
        }
    }

    /**
     * ✅ 리프레시 토큰 추출 헬퍼 메서드 (refresh 전용: null 반환 가능)
     */
    private String extractRefreshToken(String refreshTokenHeader, String authHeader) {
        String refreshToken = null;

        // 1. Refresh-Token 헤더에서 추출
        if (refreshTokenHeader != null && !refreshTokenHeader.trim().isEmpty()) {
            refreshToken = refreshTokenHeader.trim();
            if (refreshToken.regionMatches(true, 0, "Bearer ", 0, 7)) {
                refreshToken = refreshToken.substring(7).trim();
            }
        }

        // 2. Authorization 헤더에서 추출 (fallback)
        if ((refreshToken == null || refreshToken.isEmpty()) &&
                authHeader != null && authHeader.startsWith("Bearer ")) {
            refreshToken = authHeader.substring(7).trim();
        }

        return refreshToken;
    }

    /**
     * ✅ 토큰 마스킹 헬퍼 메서드 (보안 강화)
     */
    private String maskToken(String token) {
        if (token == null || token.length() < 10) {
            return "***";
        }
        return token.substring(0, 10) + "..." + token.substring(token.length() - 4);
    }

    /**
     * ✅ BusinessException 처리 헬퍼 메서드 (refresh용)
     */
    private ResponseEntity<ApiResponse<LoginResponseDto>> handleRefreshBusinessException(BusinessException e) {
        String errorCode = e.getErrorCode().getCode();
        String errorMessage = e.getMessage();

        // 인증 관련 오류 (401)
        if ("INVALID_REFRESH_TOKEN".equals(errorCode) ||
                "TOKEN_EXPIRED".equals(errorCode) ||
                "EXPIRED_ACCESS_TOKEN".equals(errorCode) ||
                "USER_NOT_FOUND".equals(errorCode)) {

            return ResponseEntity.status(401)
                    .body(ApiResponse.error(errorCode, errorMessage));
        }

        // 서버 내부 오류 (500)
        if ("INTERNAL_SERVER_ERROR".equals(errorCode)) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(errorCode, "서버 내부 오류가 발생했습니다."));
        }

        // 기타 오류 (400)
        return ResponseEntity.status(400)
                .body(ApiResponse.error(errorCode, errorMessage));
    }

    // ======== 로그아웃 ========

    @Operation(summary = "로그아웃")
    @PostMapping("/logout")
    @UserActivityLogging(actionType = "USER_LOGOUT", priority = UserActivityLogging.Priority.NORMAL, description = "사용자 로그아웃")
    public ResponseEntity<Void> logout(
            @RequestHeader(value = "Refresh-Token", required = false) String refreshHeader,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        log.info("[LOGOUT] 요청");
        // ✅ 요청하신 방식: 토큰 미제공/무효 시 예외 발생, 성공 시 204 No Content
        String refreshToken = extractRefreshTokenForLogout(refreshHeader, authorization);
        authService.logout(refreshToken);
        log.info("[LOGOUT] 토큰 무효화 완료");
        return ResponseEntity.noContent().build(); // 204
    }

    /**
     * ✅ 로그아웃 전용 토큰 추출 (없거나 부적합하면 BusinessException 발생)
     */
    private String extractRefreshTokenForLogout(String refreshHeader, String authorization) {
        // 1) Refresh-Token 우선
        if (refreshHeader != null && !refreshHeader.isBlank()) {
            String token = stripBearer(refreshHeader);
            if (token.length() >= 20)
                return token;
        }
        // 2) 호환: Authorization에 refresh가 담겨오는 경우
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7).trim();
            if (token.length() >= 20)
                return token;
        }
        throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
    }

    /**
     * ✅ Bearer 접두어 제거 (대소문자 무시)
     */
    private String stripBearer(String value) {
        String v = value.trim();
        if (v.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return v.substring(7).trim();
        }
        return v;
    }

    // ======== 모든 기기 로그아웃 ========

    @Operation(summary = "모든 기기 로그아웃")
    @PostMapping("/logout-all")
    @UserActivityLogging(actionType = "USER_LOGOUT_ALL", priority = UserActivityLogging.Priority.HIGH, description = "모든 기기 로그아웃")
    public ResponseEntity<ApiResponse<Void>> logoutAll(
            @RequestHeader(value = "X-Username", required = false) String username) {
        log.info("[LOGOUT-ALL] 요청: {}", username);

        try {
            if (username == null || username.trim().isEmpty()) {
                return ResponseEntity.status(400)
                        .body(ApiResponse.error("MISSING_USERNAME", "X-Username 헤더가 필요합니다."));
            }

            authService.logoutAll(username.trim());
            return ResponseEntity.ok(ApiResponse.success(null, "모든 기기에서 로그아웃되었습니다."));

        } catch (BusinessException e) {
            log.error("[LOGOUT-ALL] 실패 - BusinessException: {}", e.getMessage());
            return ResponseEntity.status(400)
                    .body(ApiResponse.error(e.getErrorCode().getCode(), e.getMessage()));

        } catch (Exception e) {
            log.error("[LOGOUT-ALL] 실패 - Exception: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("LOGOUT_ALL_FAILED", "전체 로그아웃 중 서버 오류가 발생했습니다."));
        }
    }

    // ======== 중복확인 ========

    @Operation(summary = "아이디(Username) 중복확인")
    @GetMapping("/check-username")
    @UserActivityLogging(actionType = "CHECK_USERNAME", priority = UserActivityLogging.Priority.NORMAL, description = "아이디 중복 확인")
    public ResponseEntity<ApiResponse<CheckUsernameResponse>> checkUsername(@RequestParam String username) {
        CheckUsernameResponse res = authService.checkUsername(username);
        return ResponseEntity.ok(ApiResponse.success(res, res.getMessage()));
    }

    @Operation(summary = "이메일 중복확인")
    @GetMapping("/check-email")
    @UserActivityLogging(actionType = "CHECK_EMAIL", priority = UserActivityLogging.Priority.NORMAL, description = "이메일 중복 확인")
    public ResponseEntity<ApiResponse<CheckEmailResponse>> checkEmail(@RequestParam String email) {
        CheckEmailResponse res = authService.checkEmail(email);
        return ResponseEntity.ok(ApiResponse.success(res, res.getMessage()));
    }

    // ======== 이메일 인증코드 (회원가입/일반 인증용) ========

    @Operation(summary = "이메일 인증코드 발송")
    @PostMapping("/send-code")
    @UserActivityLogging(actionType = "EMAIL_SEND_CODE", priority = UserActivityLogging.Priority.NORMAL, description = "이메일 인증코드 발송")
    public ResponseEntity<ApiResponse<Void>> sendCode(@RequestBody @Valid EmailSendRequest req) {
        authService.sendVerificationCode(req.getEmail());
        return ResponseEntity.ok(ApiResponse.success(null, "이메일로 인증코드가 발송되었습니다."));
    }

    @Operation(summary = "이메일 인증코드 검증")
    @PostMapping("/verify-code")
    @UserActivityLogging(actionType = "EMAIL_VERIFY_CODE", priority = UserActivityLogging.Priority.NORMAL, description = "이메일 인증코드 검증")
    public ResponseEntity<ApiResponse<Boolean>> verifyCode(@RequestBody @Valid EmailVerifyRequest req) {
        boolean valid = authService.verifyCode(req.getEmail(), req.getCode());
        String msg = valid ? "인증 성공!" : "인증코드가 올바르지 않습니다.";
        return ResponseEntity.ok(ApiResponse.success(valid, msg));
    }

    // ======== [아이디 찾기] 인증코드 및 아이디 반환 ========

    @Operation(summary = "[아이디 찾기] 인증코드 발송")
    @PostMapping("/find-id/send-code")
    @UserActivityLogging(actionType = "FIND_ID_SEND_CODE", priority = UserActivityLogging.Priority.NORMAL, description = "아이디 찾기 인증코드 발송")
    public ResponseEntity<ApiResponse<Void>> sendFindIdCode(@RequestBody @Valid EmailSendRequest req) {
        authService.sendCodeForFindId(req.getEmail());
        return ResponseEntity.ok(ApiResponse.success(null, "아이디 찾기 인증코드 발송 완료"));
    }

    @Operation(summary = "[아이디 찾기] 인증코드 검증")
    @PostMapping("/find-id/verify-code")
    @UserActivityLogging(actionType = "FIND_ID_VERIFY_CODE", priority = UserActivityLogging.Priority.NORMAL, description = "아이디 찾기 인증코드 검증")
    public ResponseEntity<ApiResponse<Boolean>> verifyFindIdCode(@RequestBody @Valid EmailVerifyRequest req) {
        boolean valid = authService.verifyFindIdCode(req.getEmail(), req.getCode());
        return ResponseEntity.ok(ApiResponse.success(valid, valid ? "인증 성공" : "인증 실패"));
    }

    @Operation(summary = "[아이디 찾기] 이메일+코드로 아이디 반환")
    @PostMapping("/find-id")
    @UserActivityLogging(actionType = "FIND_ID", priority = UserActivityLogging.Priority.HIGH, description = "아이디 찾기 완료")
    public ResponseEntity<ApiResponse<FindIdResponse>> findId(@RequestBody @Valid FindIdRequest req) {
        String username = authService.findIdByEmailAndCode(req.getEmail(), req.getCode());
        return ResponseEntity.ok(ApiResponse.success(new FindIdResponse(username), "아이디 조회 성공"));
    }
}