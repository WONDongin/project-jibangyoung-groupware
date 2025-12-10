package com.jibangyoung.domain.recommendation.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jibangyoung.domain.recommendation.dto.RecommendationResultDto;
import com.jibangyoung.domain.recommendation.service.RecommendationAlgorithmService;
import com.jibangyoung.domain.recommendation.service.RecommendationService;
import com.jibangyoung.global.annotation.UserActivityLogging;
import com.jibangyoung.global.security.CustomUserPrincipal;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/recommendation")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationAlgorithmService recommendationService;
    private final RecommendationService recommendationResultService;

    @PostMapping("/{userId}/{responseId}")
    @UserActivityLogging(actionType = "RECOMMENDATION_CREATE", scoreDelta = 30, priority = UserActivityLogging.Priority.HIGH, description = "지역 추천 생성")
    public ResponseEntity<Void> createRecommendations(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long userId,
            @PathVariable Long responseId) {

        if (!principal.getId().equals(userId)) {
            return ResponseEntity.status(403).build();
        }

        recommendationService.generateRecommendations(userId, responseId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{userId}/{responseId}")
    @UserActivityLogging(actionType = "RECOMMENDATION_VIEW", priority = UserActivityLogging.Priority.NORMAL, description = "추천 결과 조회")
    public ResponseEntity<List<RecommendationResultDto>> getRecommendations(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long userId,
            @PathVariable Long responseId) {

        if (!principal.getId().equals(userId)) {
            return ResponseEntity.status(403).build();
        }

        List<RecommendationResultDto> result = recommendationResultService.getRankedRecommendationsGroupedByRankGroup(
                userId,
                responseId);
        return ResponseEntity.ok(result);
    }
}