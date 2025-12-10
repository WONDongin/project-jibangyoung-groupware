package com.jibangyoung.domain.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jibangyoung.domain.auth.dto.PasswordUpdateRequestDto;
import com.jibangyoung.domain.auth.dto.ProfileUpdateRequestDto;
import com.jibangyoung.domain.auth.dto.UserDto;
import com.jibangyoung.global.annotation.UserActivityLogging;
import com.jibangyoung.global.security.CustomUserPrincipal;
import com.jibangyoung.domain.auth.service.UserService;
import com.jibangyoung.global.common.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Validated
@Slf4j
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @PreAuthorize("hasRole('USER')")
    @UserActivityLogging(actionType = "USER_PROFILE_VIEW", priority = UserActivityLogging.Priority.NORMAL, description = "내 프로필 조회")
    public ResponseEntity<ApiResponse<UserDto>> getCurrentUser(
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        UserDto user = userService.getUserById(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    @UserActivityLogging(actionType = "USER_PROFILE_VIEW_BY_ID", priority = UserActivityLogging.Priority.NORMAL, description = "특정 사용자 프로필 조회")
    public ResponseEntity<ApiResponse<UserDto>> getUserById(@PathVariable Long id) {
        UserDto user = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @PutMapping("/profile")
    @PreAuthorize("hasRole('USER')")
    @UserActivityLogging(actionType = "USER_PROFILE_UPDATE", priority = UserActivityLogging.Priority.HIGH, description = "프로필 정보 수정")
    public ResponseEntity<ApiResponse<UserDto>> updateProfile(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody ProfileUpdateRequestDto request) {

        UserDto updatedUser = userService.updateUserProfile(
                principal.getId(),
                request.getNickname(),
                request.getPhone(),
                request.getProfileImageUrl());

        return ResponseEntity.ok(ApiResponse.success(updatedUser, "프로필이 업데이트되었습니다."));
    }

    @PutMapping("/password")
    @PreAuthorize("hasRole('USER')")
    @UserActivityLogging(actionType = "USER_PASSWORD_UPDATE", priority = UserActivityLogging.Priority.CRITICAL, description = "비밀번호 변경")
    public ResponseEntity<ApiResponse<Void>> updatePassword(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody PasswordUpdateRequestDto request) {

        userService.updateUserPassword(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(null, "비밀번호가 업데이트되었습니다."));
    }

    @PostMapping("/deactivate")
    @PreAuthorize("hasRole('USER')")
    @UserActivityLogging(actionType = "USER_DEACTIVATE", scoreDelta = -100, priority = UserActivityLogging.Priority.CRITICAL, description = "계정 비활성화")
    public ResponseEntity<ApiResponse<Void>> deactivateAccount(
            @AuthenticationPrincipal CustomUserPrincipal principal) {

        userService.deactivateUser(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(null, "계정이 비활성화되었습니다."));
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    @UserActivityLogging(actionType = "ADMIN_USER_ACTIVATE", scoreDelta = 100, priority = UserActivityLogging.Priority.CRITICAL, description = "관리자 계정 활성화")
    public ResponseEntity<ApiResponse<Void>> activateAccount(@PathVariable Long id) {
        userService.activateUser(id);
        return ResponseEntity.ok(ApiResponse.success(null, "계정이 활성화되었습니다."));
    }

    @GetMapping("/check-username")
    @UserActivityLogging(actionType = "CHECK_USERNAME_AVAILABILITY", priority = UserActivityLogging.Priority.NORMAL, description = "사용자명 사용 가능성 확인")
    public ResponseEntity<ApiResponse<Boolean>> checkUsernameAvailability(
            @RequestParam String username) {

        boolean available = userService.isUsernameAvailable(username);
        return ResponseEntity.ok(ApiResponse.success(available,
                available ? "사용 가능한 사용자명입니다." : "이미 사용 중인 사용자명입니다."));
    }

    @GetMapping("/check-email")
    @UserActivityLogging(actionType = "CHECK_EMAIL_AVAILABILITY", priority = UserActivityLogging.Priority.NORMAL, description = "이메일 사용 가능성 확인")
    public ResponseEntity<ApiResponse<Boolean>> checkEmailAvailability(
            @RequestParam String email) {

        boolean available = userService.isEmailAvailable(email);
        return ResponseEntity.ok(ApiResponse.success(available,
                available ? "사용 가능한 이메일입니다." : "이미 사용 중인 이메일입니다."));
    }
}