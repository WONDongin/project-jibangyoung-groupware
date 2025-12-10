package com.jibangyoung.domain.policy.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jibangyoung.domain.policy.dto.PolicyCardDto;
import com.jibangyoung.domain.policy.dto.PolicyFavoriteDto;
import com.jibangyoung.domain.policy.service.PolicyFavoriteService;
import com.jibangyoung.domain.policy.service.PolicyService;
import com.jibangyoung.global.annotation.UserActivityLogging;
import com.jibangyoung.global.security.CustomUserPrincipal;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/policy")
@RequiredArgsConstructor
public class PolicyFavoriteController {

    private final PolicyFavoriteService policyFavoriteService;
    private final PolicyService policyService;

    @PostMapping("/sync")
    @UserActivityLogging(actionType = "POLICY_FAVORITE_SYNC", priority = UserActivityLogging.Priority.HIGH, description = "정책 북마크 동기화")
    public ResponseEntity<String> syncBookmarks(@RequestBody PolicyFavoriteDto request,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        try {
            policyFavoriteService.syncBookmarks(request.userId(), request.bookmarkedPolicyIds());
            return ResponseEntity.ok("북마크가 성공적으로 동기화되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("북마크 동기화 실패: " + e.getMessage());
        }
    }

    @GetMapping("/favorites/{userId}")
    @UserActivityLogging(actionType = "POLICY_FAVORITES_VIEW", priority = UserActivityLogging.Priority.NORMAL, description = "북마크된 정책 코드 조회")
    public ResponseEntity<List<Long>> getBookmarkedPolicyCodes(@PathVariable Long userId,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        try {
            List<Long> bookmarkedPolicyCodes = policyFavoriteService.findPolicyCodesByUserId(userId);
            return ResponseEntity.ok(bookmarkedPolicyCodes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/favorites/policylist")
    @UserActivityLogging(actionType = "POLICY_FAVORITES_LIST_VIEW", priority = UserActivityLogging.Priority.NORMAL, description = "찜한 정책 상세 목록 조회")
    public List<PolicyCardDto> getPoliciesByNos(@RequestBody List<Integer> policyNos,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        return policyService.getPoliciesByCodes(policyNos);
    }
}