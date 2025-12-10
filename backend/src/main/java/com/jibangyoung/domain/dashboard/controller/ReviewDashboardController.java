package com.jibangyoung.domain.dashboard.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jibangyoung.domain.dashboard.dto.ReviewPostDto;
import com.jibangyoung.domain.dashboard.service.ReviewDashboardService;
import com.jibangyoung.global.annotation.UserActivityLogging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class ReviewDashboardController {

    private final ReviewDashboardService reviewDashboardService;

    @GetMapping("/review-top/top3")
    @UserActivityLogging(actionType = "REVIEW_TOP3_VIEW", priority = UserActivityLogging.Priority.NORMAL, description = "인기 정착 후기 Top3 조회")
    public ResponseEntity<List<ReviewPostDto>> getReviewTop3() {
        try {
            List<ReviewPostDto> result = reviewDashboardService.getReviewTop3();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("인기 정착 후기 Top3 조회 API 오류", e);
            return ResponseEntity.ok(List.of());
        }
    }
}
