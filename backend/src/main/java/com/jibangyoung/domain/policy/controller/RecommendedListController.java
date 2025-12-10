package com.jibangyoung.domain.policy.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jibangyoung.domain.policy.dto.PolicyCardDto;
import com.jibangyoung.domain.policy.service.RecommendedListService;
import com.jibangyoung.global.annotation.UserActivityLogging;
import com.jibangyoung.global.security.CustomUserPrincipal;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/policy")
@RequiredArgsConstructor
public class RecommendedListController {

    private final RecommendedListService recommendedListService;

    @GetMapping("/recList")
    @UserActivityLogging(actionType = "POLICY_RECOMMEND_LIST_VIEW", priority = UserActivityLogging.Priority.NORMAL, description = "추천 정책 목록 조회")
    public List<PolicyCardDto> getRecommendedPolicies(@RequestParam String userId,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        return recommendedListService.getRecommendedPoliciesByUserId(userId);
    }
}