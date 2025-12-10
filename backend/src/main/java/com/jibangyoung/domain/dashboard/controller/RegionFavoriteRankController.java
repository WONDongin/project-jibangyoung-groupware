package com.jibangyoung.domain.dashboard.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jibangyoung.domain.dashboard.dto.RegionFavoriteRankDto;
import com.jibangyoung.domain.dashboard.service.RegionFavoriteRankService;
import com.jibangyoung.global.annotation.UserActivityLogging;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Dashboard", description = "정책 찜 수 기준 인기 지역 TOP 10 API")
@RestController
@RequestMapping("/api/dashboard/region")
@RequiredArgsConstructor
public class RegionFavoriteRankController {

    private final RegionFavoriteRankService service;

    @Operation(summary = "정책 찜 수 기준 인기 지역 TOP 10 조회 (Redis 캐시)")
    @GetMapping("/top10")
    @UserActivityLogging(actionType = "REGION_FAVORITE_TOP10_VIEW", priority = UserActivityLogging.Priority.NORMAL, description = "인기 지역 TOP 10 조회")
    public ResponseEntity<List<RegionFavoriteRankDto>> getTop10RegionFavorites() {
        List<RegionFavoriteRankDto> ranks = service.getTop10FromCache();
        return ResponseEntity.ok(ranks);
    }
}