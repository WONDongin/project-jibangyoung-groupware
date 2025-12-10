package com.jibangyoung.domain.community.controller;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jibangyoung.domain.community.dto.CommentRequestDto;
import com.jibangyoung.domain.community.dto.CommentResponseDto;
import com.jibangyoung.domain.community.dto.PostCreateRequestDto;
import com.jibangyoung.domain.community.dto.PostDetailDto;
import com.jibangyoung.domain.community.dto.PostListDto;
import com.jibangyoung.domain.community.dto.PostUpdateRequestDto;
import com.jibangyoung.domain.community.dto.PresignedUrlRequest;
import com.jibangyoung.domain.community.dto.PresignedUrlResponse;
import com.jibangyoung.domain.community.dto.RecommendationRequestDto;
import com.jibangyoung.domain.community.dto.RegionResponseDto;
import com.jibangyoung.domain.community.dto.ReportCreateRequestDto;
import com.jibangyoung.domain.community.service.CommunityService;
import com.jibangyoung.domain.community.service.PresignedUrlService;
import com.jibangyoung.domain.community.service.ReportService;
import com.jibangyoung.global.annotation.UserActivityLogging;
import com.jibangyoung.global.common.ApiResponse;
import com.jibangyoung.global.security.CustomUserPrincipal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/community")
@RequiredArgsConstructor
@Slf4j
public class CommunityController {

    private final CommunityService communityService;
    private final PresignedUrlService presignedUrlService;
    private final ReportService reportService;

    // 지역 코드
    @GetMapping("/region")
    @UserActivityLogging(actionType = "REGION_CODE_VIEW", priority = UserActivityLogging.Priority.NORMAL, description = "지역 코드 목록 조회")
    public List<RegionResponseDto> getRegionCodes() {
        return communityService.getAllRegionsBoard();
    }

    // period 인기순
    @GetMapping("/top-liked")
    @UserActivityLogging(actionType = "TOP_LIKED_VIEW", priority = UserActivityLogging.Priority.NORMAL, description = "기간별 인기글 조회")
    public List<PostListDto> getTopLikedByPeriod(@RequestParam(defaultValue = "today") String period) {
        return communityService.getCachedTop10ByPeriod(period);
    }

    // 최신 인기글
    @GetMapping("/popular")
    @UserActivityLogging(actionType = "POPULAR_POSTS_VIEW", priority = UserActivityLogging.Priority.NORMAL, description = "인기글 목록 조회")
    public Page<PostListDto> getPopularPosts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return communityService.getPopularPostsPage(page, size);
    }

    // 지역 게시판
    @GetMapping("/region/{regionCode}")
    @UserActivityLogging(actionType = "REGION_POSTS_VIEW", priority = UserActivityLogging.Priority.NORMAL, description = "지역별 게시글 목록 조회")
    public Page<PostListDto> getPostsByRegion(
            @PathVariable String regionCode,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String searchType) {
        return communityService.getPostsByRegion(regionCode, page, size, category, search, searchType);
    }

    // 게시글 상세
    @GetMapping("/post/{postId}")
    @UserActivityLogging(actionType = "POST_DETAIL_VIEW", priority = UserActivityLogging.Priority.NORMAL, description = "게시글 상세 조회")
    public PostDetailDto getPostDetail(@PathVariable Long postId) {
        return communityService.getPostDetail(postId);
    }

    // s3 이미지
    @PostMapping("/presign")
    @UserActivityLogging(actionType = "PRESIGNED_URL_GENERATE", priority = UserActivityLogging.Priority.NORMAL, description = "S3 이미지 업로드용 Presigned URL 생성")
    public PresignedUrlResponse getPresignedUrl(@RequestBody PresignedUrlRequest request) {
        String fileName = "temp/" + request.getFileName();
        String presignedUrl = presignedUrlService.generatePresignedUrl(fileName, request.getContentType());
        String publicUrl = presignedUrlService.getPublicUrl(fileName);

        return new PresignedUrlResponse(presignedUrl, publicUrl);
    }

    // 글작성
    @PostMapping("/write")
    @PreAuthorize("isAuthenticated()")
    @UserActivityLogging(actionType = "POST", scoreDelta = 10, priority = UserActivityLogging.Priority.HIGH, description = "게시글 작성")
    public ResponseEntity<ApiResponse<Long>> writePost(@RequestBody @Valid PostCreateRequestDto request) {
        Long postId = communityService.write(request);
        return ResponseEntity.ok(ApiResponse.success(postId));
    }

    @GetMapping("/regionPopular/{regionCode}")
    @UserActivityLogging(actionType = "REGION_POPULAR_VIEW", priority = UserActivityLogging.Priority.NORMAL, description = "지역별 인기글 조회")
    public Page<PostListDto> getPostsByRegionPopular(
            @PathVariable String regionCode,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return communityService.getPostsByRegionPopular(regionCode, page, size);
    }

    // 후기 추천순
    @GetMapping("/popular/reviews")
    @UserActivityLogging(actionType = "REVIEW_POSTS_VIEW", priority = UserActivityLogging.Priority.NORMAL, description = "후기 추천순 게시글 조회")
    public List<PostListDto> getTopReviewPosts() {
        return communityService.getTopReviewPosts();
    }

    // 공지
    @GetMapping("/notices")
    @UserActivityLogging(actionType = "NOTICES_VIEW", priority = UserActivityLogging.Priority.NORMAL, description = "공지사항 목록 조회")
    public ResponseEntity<List<PostListDto>> getNotices() {
        return ResponseEntity.ok(communityService.getNotices());
    }

    // 지역별 공지사항 조회
    @GetMapping("/region/{regionCode}/notices")
    @UserActivityLogging(actionType = "REGION_NOTICES_VIEW", priority = UserActivityLogging.Priority.NORMAL, description = "지역별 공지사항 조회")
    public ResponseEntity<List<PostListDto>> getNoticesByRegion(@PathVariable String regionCode) {
        Long regionId = Long.parseLong(regionCode);
        return ResponseEntity.ok(communityService.getNoticesByRegion(regionId));
    }

    // 지역별 인기글 조회
    @GetMapping("/region/{regionCode}/popular")
    @UserActivityLogging(actionType = "REGION_POPULAR_POSTS_VIEW", priority = UserActivityLogging.Priority.NORMAL, description = "지역별 인기글 조회")
    public ResponseEntity<List<PostListDto>> getPopularPostsByRegion(@PathVariable String regionCode) {
        Long regionId = Long.parseLong(regionCode);
        return ResponseEntity.ok(communityService.getPopularPostsByRegion(regionId));
    }

    // 댓글 가져오기
    @GetMapping("/posts/{postId}/comments")
    @UserActivityLogging(actionType = "COMMENTS_VIEW", priority = UserActivityLogging.Priority.NORMAL, description = "댓글 목록 조회")
    public ResponseEntity<List<CommentResponseDto>> getComments(@PathVariable Long postId) {
        return ResponseEntity.ok(communityService.findCommentsByPostId(postId));
    }

    // 댓글 작성
    @PostMapping("/posts/{postId}/comments")
    @PreAuthorize("isAuthenticated()")
    @UserActivityLogging(actionType = "COMMENT", scoreDelta = 2, priority = UserActivityLogging.Priority.NORMAL, description = "댓글 작성")
    public ResponseEntity<Void> createComment(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @PathVariable Long postId,
            @RequestBody CommentRequestDto requestDto) {
        if (userPrincipal == null) {
            log.warn("인증된 사용자 정보가 없습니다.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Long userId = userPrincipal.getId();
        log.info("댓글 작성, userId: {}", userId);
        communityService.saveComment(postId, userId, requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // 댓글 삭제
    @DeleteMapping("/comments/{commentId}")
    @PreAuthorize("isAuthenticated()")
    @UserActivityLogging(actionType = "COMMENT_DELETE", scoreDelta = -2, priority = UserActivityLogging.Priority.HIGH, description = "댓글 삭제")
    public ResponseEntity<Void> deleteComment(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @PathVariable Long commentId) {
        if (userPrincipal == null) {
            log.warn("인증된 사용자 정보가 없습니다.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Long userId = userPrincipal.getId();
        communityService.deleteComment(commentId, userId);
        return ResponseEntity.noContent().build();
    }

    // 게시글 수정
    @PutMapping("/post/{postId}")
    @PreAuthorize("isAuthenticated()")
    @UserActivityLogging(actionType = "POST_UPDATE", priority = UserActivityLogging.Priority.HIGH, description = "게시글 수정")
    public ResponseEntity<Void> updatePost(
            @PathVariable Long postId,
            @RequestBody @Valid PostUpdateRequestDto requestDto,
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            log.warn("인증된 사용자 정보가 없습니다.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Long userId = userPrincipal.getId();
        boolean isAdmin = userPrincipal.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        communityService.updatePost(postId, userId, requestDto, isAdmin);
        return ResponseEntity.ok().build();
    }

    // 게시글 삭제
    @DeleteMapping("/post/{postId}")
    @PreAuthorize("isAuthenticated()")
    @UserActivityLogging(actionType = "POST_DELETE", scoreDelta = -10, priority = UserActivityLogging.Priority.HIGH, description = "게시글 삭제")
    public ResponseEntity<Void> deletePost(
            @PathVariable Long postId,
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            log.warn("인증된 사용자 정보가 없습니다.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Long userId = userPrincipal.getId();
        boolean isAdmin = userPrincipal.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        log.info("게시글 삭제 요청, postId: {}, userId: {}", postId, userId);
        communityService.deletePost(postId, userId, isAdmin);
        return ResponseEntity.noContent().build();
    }

    // 게시글 추천
    @PostMapping("/post/{postId}/recommend")
    @PreAuthorize("isAuthenticated()")
    @UserActivityLogging(actionType = "POST_RECOMMEND", scoreDelta = 1, priority = UserActivityLogging.Priority.NORMAL, description = "게시글 추천")
    public ResponseEntity<Void> recommendPost(
            @PathVariable Long postId,
            @RequestBody @Valid RecommendationRequestDto requestDto,
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            log.warn("인증된 사용자 정보가 없습니다.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        communityService.recommendPost(postId, userPrincipal.getId(), requestDto.getType());
        return ResponseEntity.ok().build();
    }

    // 사용자의 게시글 추천 상태 조회
    @PostMapping("/user/recommendation/status")
    @PreAuthorize("isAuthenticated()")
    @UserActivityLogging(actionType = "RECOMMENDATION_STATUS_VIEW", priority = UserActivityLogging.Priority.NORMAL, description = "게시글 추천 상태 조회")
    public ResponseEntity<String> getUserRecommendation(
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        Long postId = Long.valueOf(request.get("postId").toString());
        if (userPrincipal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String recommendationType = communityService.getUserRecommendationType(postId, userPrincipal.getId());
        return ResponseEntity.ok(recommendationType != null ? recommendationType : "");
    }

    // 게시글의 각 추천 유형별 개수 조회
    @GetMapping("/post/{postId}/recommendation-counts")
    @UserActivityLogging(actionType = "RECOMMENDATION_COUNTS_VIEW", priority = UserActivityLogging.Priority.NORMAL, description = "게시글 추천 개수 조회")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getRecommendationCounts(@PathVariable Long postId) {
        Map<String, Long> counts = communityService.getRecommendationCounts(postId);
        return ResponseEntity.ok(ApiResponse.success(counts));
    }

    // 조회수 증가
    @PostMapping("/post/{postId}/view")
    public ResponseEntity<Void> increaseViewCount(@PathVariable Long postId) {
        communityService.increaseViewCount(postId);
        return ResponseEntity.ok().build();
    }

    // 신고 접수
    @PostMapping("/report")
    @PreAuthorize("isAuthenticated()")
    @UserActivityLogging(actionType = "REPORT_CREATE", priority = UserActivityLogging.Priority.CRITICAL, description = "신고 접수")
    public ResponseEntity<ApiResponse<Void>> createReport(
            @RequestBody @Valid ReportCreateRequestDto requestDto,
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            log.warn("인증된 사용자 정보가 없습니다.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            reportService.createReport(userPrincipal.getId(), requestDto);
            return ResponseEntity.ok(ApiResponse.success(null, "신고가 접수되었습니다."));
        } catch (Exception e) {
            log.warn("신고 접수 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("REPORT_FAILED", e.getMessage()));
        }
    }
}