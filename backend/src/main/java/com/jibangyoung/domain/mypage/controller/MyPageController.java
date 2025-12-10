package com.jibangyoung.domain.mypage.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jibangyoung.domain.mypage.dto.ActivityEventDto;
//import com.jibangyoung.domain.mypage.dto.AlertInfoDto;
import com.jibangyoung.domain.mypage.dto.CommentPreviewDto;
import com.jibangyoung.domain.mypage.dto.MyRegionScoreDto;
import com.jibangyoung.domain.mypage.dto.MyReportDto;
import com.jibangyoung.domain.mypage.dto.QuicklinkMenuItemDto;
import com.jibangyoung.domain.mypage.dto.RecommendRegionResultDto;
import com.jibangyoung.domain.mypage.dto.RegionScoreDto;
import com.jibangyoung.domain.mypage.dto.SidebarMenuItemDto;
import com.jibangyoung.domain.mypage.dto.SurveyAnswerDto;
import com.jibangyoung.domain.mypage.dto.SurveyResponseGroupsResponse;
import com.jibangyoung.domain.mypage.dto.UserProfileDto;
// com.jibangyoung.domain.mypage.service.AlertQueryService;
import com.jibangyoung.domain.mypage.service.CommentService;
import com.jibangyoung.domain.mypage.service.MyReportService;
import com.jibangyoung.domain.mypage.service.PostService;
import com.jibangyoung.domain.mypage.service.ProfileService;
import com.jibangyoung.domain.mypage.service.ScoreService;
import com.jibangyoung.domain.mypage.service.SurveyResponseService;
import com.jibangyoung.domain.mypage.support.SidebarMenuFactory;
import com.jibangyoung.global.annotation.UserActivityLogging;
import com.jibangyoung.global.common.ApiResponse;
import com.jibangyoung.global.security.CustomUserPrincipal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Tag(name = "마이페이지", description = "마이페이지 통합 API")
@RestController
@RequestMapping("/api/mypage")
@RequiredArgsConstructor
public class MyPageController {

    // 서비스 주입
    //private final AlertQueryService alertQueryService;
    private final CommentService commentService;
    private final PostService postService;
    private final MyReportService myReportService;
    private final ProfileService profileService;
    private final ScoreService scoreService;
    private final SurveyResponseService surveyResponseService;

    // ========================================
    // 프로필 관련 API
    // ========================================

    @Operation(summary = "내 프로필 조회")
    @GetMapping("/users/{userId}/profile")
    @UserActivityLogging(actionType = "PROFILE_VIEW", priority = UserActivityLogging.Priority.NORMAL, description = "프로필 조회")
    public ApiResponse<UserProfileDto> getUserProfile(@PathVariable Long userId) {
        return ApiResponse.success(profileService.getUserProfile(userId));
    }

    @Operation(summary = "내 프로필 수정")
    @PatchMapping("/users/{userId}/profile")
    @UserActivityLogging(actionType = "PROFILE_UPDATE", scoreDelta = 1, priority = UserActivityLogging.Priority.HIGH, description = "프로필 정보 수정")
    public ApiResponse<Void> updateUserProfile(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateProfileRequest request) {
        profileService.updateUserProfile(
                userId,
                request.getNickname(),
                request.getPhone(),
                request.getProfileImageUrl(),
                request.getRegion());
        return ApiResponse.success(null);
    }

    // ========================================
    // 알림 관련 API
    // ========================================

//    @Operation(summary = "관심지역 알림 목록 조회 (Slice 기반)")
//    @GetMapping("/users/{userId}/alerts")
//    @UserActivityLogging(actionType = "ALERT_LIST_VIEW", priority = UserActivityLogging.Priority.NORMAL, description = "알림 목록 조회")
//    public ApiResponse<Slice<AlertInfoDto>> getUserAlerts(
//            @PathVariable Long userId,
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "10") int size) {
//        return ApiResponse.success(alertQueryService.getUserAlerts(userId, page, size));
//    }

    // ========================================
    // 댓글 관련 API
    // ========================================

    @Operation(summary = "내 댓글 목록 조회")
    @GetMapping("/users/{userId}")
    @UserActivityLogging(actionType = "MY_COMMENT_LIST_VIEW", priority = UserActivityLogging.Priority.NORMAL, description = "내 댓글 목록 조회")
    public ApiResponse<Page<CommentPreviewDto>> getMyComments(
            @PathVariable Long userId,
            Pageable pageable) {
        Page<CommentPreviewDto> comments = commentService.getMyComments(userId, pageable);
        return ApiResponse.success(comments);
    }

    @Operation(summary = "내 댓글 소프트 삭제")
    @DeleteMapping("/users/{userId}/{commentId}")
    @UserActivityLogging(actionType = "COMMENT_DELETE", scoreDelta = -1, priority = UserActivityLogging.Priority.HIGH, description = "댓글 삭제")
    public ResponseEntity<Void> deleteMyComment(
            @PathVariable Long userId,
            @PathVariable Long commentId) {
        commentService.deleteMyComment(userId, commentId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "내 댓글 전체 목록 조회 (삭제된 것 포함)")
    @GetMapping("/users/{userId}/all")
    @UserActivityLogging(actionType = "MY_COMMENT_ALL_VIEW", priority = UserActivityLogging.Priority.NORMAL, description = "내 댓글 전체 목록 조회 (삭제된 것 포함)")
    public ApiResponse<Page<CommentPreviewDto>> getAllMyCommentsIncludeDeleted(
            @PathVariable Long userId,
            Pageable pageable) {
        Page<CommentPreviewDto> comments = commentService.getAllMyCommentsIncludeDeleted(userId, pageable);
        return ApiResponse.success(comments);
    }

    // ========================================
    // 게시글 관련 API
    // ========================================

    @Operation(summary = "내 게시글 목록 조회 (페이징)")
    @GetMapping("/users/{userId}/posts")
    @UserActivityLogging(actionType = "MY_POST_LIST_VIEW", priority = UserActivityLogging.Priority.NORMAL, description = "내 게시글 목록 조회")
    public ApiResponse<PostService.PostListResponse> getMyPosts(
            @PathVariable long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(postService.getMyPosts(userId, page, size));
    }

    // ========================================
    // 신고 이력 관련 API
    // ========================================

    @Operation(summary = "내 신고 이력 전체 조회", description = "로그인 사용자의 신고 이력(최신순) 목록을 반환합니다.")
    @GetMapping("/reports")
    @UserActivityLogging(actionType = "REPORT_HISTORY_VIEW", priority = UserActivityLogging.Priority.HIGH, description = "신고 이력 조회")
    public ResponseEntity<List<MyReportDto>> getMyReports(
            @RequestParam Long userId) {
        List<MyReportDto> reports = myReportService.getMyReports(userId);
        return ResponseEntity.ok(reports);
    }

    // ========================================
    // 지역 점수/랭킹 관련 API (수정된 부분)
    // ========================================

    @Operation(summary = "특정 지역의 단일 점수 요약", description = "regionId로 해당 지역의 내 점수 상세 조회")
    @GetMapping("/region-score/{regionId}")
    @UserActivityLogging(actionType = "REGION_SCORE_VIEW", priority = UserActivityLogging.Priority.NORMAL, description = "지역 점수 조회")
    public ApiResponse<RegionScoreDto> getRegionScore(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @PathVariable Long regionId) { // int → Long 변경
        Long userId = userPrincipal.getId();
        return ApiResponse.success(scoreService.getRegionScore(userId, regionId));
    }

    @Operation(summary = "지역별 TOP-N 랭킹", description = "특정 지역의 상위 점수 유저 목록 조회")
    @GetMapping("/region-score/ranking")
    @UserActivityLogging(actionType = "REGION_RANKING_VIEW", priority = UserActivityLogging.Priority.NORMAL, description = "지역 랭킹 조회")
    public ApiResponse<List<RegionScoreDto>> getRegionRanking(
            @RequestParam Long regionId, // int → Long 변경
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(scoreService.getTopRankByRegion(regionId, size));
    }

    @Operation(summary = "내 모든 지역별 점수", description = "JWT 인증을 통해 로그인한 사용자의 모든 지역 점수 반환")
    @GetMapping("/region-score/my")
    @UserActivityLogging(actionType = "MY_REGION_SCORE_VIEW", priority = UserActivityLogging.Priority.NORMAL, description = "내 지역별 점수 조회")
    public ApiResponse<List<MyRegionScoreDto>> getMyRegionScores(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        Long userId = userPrincipal.getId();
        return ApiResponse.success(scoreService.getUserRegionScores(userId));
    }

    @Operation(summary = "사용자 행동 이벤트 기록", description = "점수 변동 이벤트 로깅 (비동기/배치 연동 가능)")
    @PostMapping("/region-score/activity")
    @UserActivityLogging(actionType = "ACTIVITY_RECORD", scoreDelta = 0, priority = UserActivityLogging.Priority.CRITICAL, description = "사용자 활동 이벤트 기록")
    public ApiResponse<Void> recordActivity(@RequestBody ActivityEventDto dto) {
        scoreService.recordUserActivity(dto);
        return ApiResponse.success(null);
    }

    // ========================================
    // 메뉴/사이드바 관련 API
    // ========================================

    @Operation(summary = "사이드바 메뉴 목록(권한별)")
    @GetMapping("/menu/sidebar")
    @UserActivityLogging(actionType = "SIDEBAR_MENU_VIEW", priority = UserActivityLogging.Priority.NORMAL, description = "사이드바 메뉴 조회")
    public ApiResponse<List<SidebarMenuItemDto>> getSidebarMenu(
            @RequestParam(defaultValue = "USER") String role) {
        List<SidebarMenuItemDto> result = SidebarMenuFactory.getSidebarMenu().stream()
                .filter(item -> item.roles() == null || item.roles().contains(role))
                .collect(Collectors.toList());
        return ApiResponse.success(result);
    }

    @Operation(summary = "사이드바 퀵링크 목록(권한별)")
    @GetMapping("/menu/quicklinks")
    @UserActivityLogging(actionType = "QUICKLINK_MENU_VIEW", priority = UserActivityLogging.Priority.NORMAL, description = "퀵링크 메뉴 조회")
    public ApiResponse<List<QuicklinkMenuItemDto>> getQuicklinks(
            @RequestParam(defaultValue = "USER") String role) {
        List<QuicklinkMenuItemDto> result = SidebarMenuFactory.getQuicklinks().stream()
                .filter(item -> item.roles() == null || item.roles().contains(role))
                .collect(Collectors.toList());
        return ApiResponse.success(result);
    }

    // ========================================
    // 설문 응답 관련 API
    // ========================================

    @Operation(summary = "설문 응답 묶음(그룹) 페이징 조회")
    @GetMapping("/survey-response-groups")
    @UserActivityLogging(actionType = "SURVEY_RESPONSE_GROUP_VIEW", priority = UserActivityLogging.Priority.NORMAL, description = "설문 응답 그룹 조회")
    public ApiResponse<SurveyResponseGroupsResponse> getSurveyResponseGroups(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(surveyResponseService.getSurveyResponseGroups(userId, page, size));
    }

    @Operation(summary = "설문 묶음 상세(문항별) 조회")
    @GetMapping("/survey-responses/{responseId}/answers")
    @UserActivityLogging(actionType = "SURVEY_ANSWER_VIEW", priority = UserActivityLogging.Priority.NORMAL, description = "설문 답변 상세 조회")
    public ApiResponse<List<SurveyAnswerDto>> getSurveyAnswers(
            @PathVariable Long responseId) {
        return ApiResponse.success(surveyResponseService.getSurveyAnswersByResponseId(responseId));
    }

    @Operation(summary = "추천지역 산출")
    @GetMapping("/survey-responses/{responseId}/recommend-region")
    @UserActivityLogging(actionType = "REGION_RECOMMEND", scoreDelta = 3, priority = UserActivityLogging.Priority.HIGH, description = "지역 추천 결과 조회")
    public ApiResponse<RecommendRegionResultDto> getRecommendRegion(
            @PathVariable Long responseId) {
        return ApiResponse.success(surveyResponseService.recommendRegion(responseId));
    }

    // ========================================
    // 내부 DTO 클래스
    // ========================================

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateProfileRequest {
        @Size(min = 1, max = 16)
        private String nickname;
        private String phone;
        private String profileImageUrl;
        private String region;
    }
}