package com.jibangyoung.domain.mentor.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.jibangyoung.domain.community.dto.PresignedUrlRequest;
import com.jibangyoung.domain.community.dto.PresignedUrlResponse;
import com.jibangyoung.domain.community.service.PresignedUrlService;
import com.jibangyoung.domain.mentor.dto.AdMentorLogListDTO;
import com.jibangyoung.domain.mentor.dto.AdMentorRejectRequestDTO;
import com.jibangyoung.domain.mentor.dto.AdMentorReportDTO;
import com.jibangyoung.domain.mentor.dto.AdMentorRequestDTO;
import com.jibangyoung.domain.mentor.dto.AdMentorUserDTO;
import com.jibangyoung.domain.mentor.dto.MentorApplicationRequestDto;
import com.jibangyoung.domain.mentor.dto.MentorApplicationResponseDto;
import com.jibangyoung.domain.mentor.dto.MentorNoticeCreateDto;
import com.jibangyoung.domain.mentor.dto.MentorNoticeDto;
import com.jibangyoung.domain.mentor.dto.MentorNoticeNavigationDto;
import com.jibangyoung.domain.mentor.repository.AdMentorUserRepository;
import com.jibangyoung.domain.mentor.service.AdMentorLogListService;
import com.jibangyoung.domain.mentor.service.AdMentorReportService;
import com.jibangyoung.domain.mentor.service.AdMentorRequestService;
import com.jibangyoung.domain.mentor.service.AdMentorUserService;
import com.jibangyoung.domain.mentor.service.MentorApplicationService;
import com.jibangyoung.domain.mentor.service.MentorNoticeService;
import com.jibangyoung.global.annotation.UserActivityLogging;
import com.jibangyoung.global.common.ApiResponse;
import com.jibangyoung.global.security.CustomUserPrincipal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mentor")
public class AdMentorUserController {

    private final AdMentorUserService adMentorUserService;
    private final AdMentorUserRepository adMentorUserRepository;
    private final AdMentorLogListService adMentorLogListService;
    private final AdMentorReportService adMentorReportService;
    private final AdMentorRequestService adMentorRequestService;
    private final MentorApplicationService mentorApplicationService;
    private final PresignedUrlService presignedUrlService;
    private final MentorNoticeService mentorNoticeService;

    // 🔒 멘토 권한 전용
    @GetMapping("/local")
    @PreAuthorize("hasAnyRole('MENTOR_A', 'MENTOR_B', 'MENTOR_C', 'ADMIN')")
    @UserActivityLogging(actionType = "MENTOR_LOCAL_USERS_VIEW", priority = UserActivityLogging.Priority.HIGH, description = "멘토 지역 사용자 목록 조회")
    public List<AdMentorUserDTO> getUsersByMentorRegion(@AuthenticationPrincipal CustomUserPrincipal loginUser) {
        boolean isAdmin = loginUser.getAuthorities().stream()
                .anyMatch(a -> {
                    String auth = a.getAuthority();
                    return "ADMIN".equals(auth) || "ROLE_ADMIN".equals(auth);
                });
        if (isAdmin) {
            return adMentorUserService.getAllMentorUsers();
        }

        return adMentorUserService.getAdMentorId(loginUser.getId());
    }

    @GetMapping("/logList")
    @PreAuthorize("hasAnyRole('MENTOR_A', 'MENTOR_B', 'ADMIN')")
    @UserActivityLogging(actionType = "MENTOR_LOG_LIST_VIEW", priority = UserActivityLogging.Priority.HIGH, description = "멘토 로그 목록 조회")
    public List<AdMentorLogListDTO> getMentorLogList(@AuthenticationPrincipal CustomUserPrincipal loginUser) {
        boolean isAdmin = loginUser.getAuthorities().stream()
                .anyMatch(a -> {
                    String auth = a.getAuthority();
                    return "ADMIN".equals(auth) || "ROLE_ADMIN".equals(auth);
                });
        return adMentorLogListService.getMentorLogList(loginUser.getId(), isAdmin);
    }

    @GetMapping("/report")
    @PreAuthorize("hasAnyRole('MENTOR_A', 'MENTOR_B', 'MENTOR_C', 'ADMIN')")
    @UserActivityLogging(actionType = "MENTOR_REPORT_VIEW", priority = UserActivityLogging.Priority.HIGH, description = "멘토 신고 목록 조회")
    public List<AdMentorReportDTO> getMentorRegionReports(
            @AuthenticationPrincipal CustomUserPrincipal loginUser,
            @RequestParam("type") String type) {
        return adMentorReportService.getReportsByMentorRegionAndType(loginUser.getId(), type);
    }

    @PatchMapping("/report/{id}/status")
    @PreAuthorize("hasAnyRole('MENTOR_A', 'MENTOR_B', 'MENTOR_C', 'ADMIN')")
    @UserActivityLogging(actionType = "MENTOR_REPORT_STATUS_UPDATE", priority = UserActivityLogging.Priority.CRITICAL, description = "멘토 신고 상태 변경")
    public ResponseEntity<?> updateReportStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal CustomUserPrincipal loginUser) {
        adMentorReportService.updateReportStatus(id, request.get("status"), loginUser.getId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/request/list")
    @PreAuthorize("hasAnyRole('ADMIN', 'MENTOR_A', 'MENTOR_B', 'MENTOR_C')")
    @UserActivityLogging(actionType = "MENTOR_REQUEST_LIST_VIEW", priority = UserActivityLogging.Priority.HIGH, description = "멘토 신청 목록 조회")
    public ResponseEntity<ApiResponse<List<AdMentorRequestDTO>>> getMentorApplicationList(
            @AuthenticationPrincipal CustomUserPrincipal loginUser) {
        Long loginUserId = loginUser.getId();
        List<AdMentorRequestDTO> list = adMentorRequestService.getMentorRequestsByUserRegion(loginUserId);

        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @PatchMapping("/request/{id}/approve/first")
    @PreAuthorize("hasAnyRole('MENTOR_B','MENTOR_A','ADMIN')")
    @UserActivityLogging(actionType = "MENTOR_APPROVE_FIRST", priority = UserActivityLogging.Priority.CRITICAL, description = "멘토 1차 승인")
    public ResponseEntity<?> approveFirst(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserPrincipal loginUser) {
        adMentorRequestService.approveFirst(id, loginUser.getId());
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/request/{id}/approve/second")
    @PreAuthorize("hasAnyRole('MENTOR_A', 'ADMIN')")
    @UserActivityLogging(actionType = "MENTOR_APPROVE_SECOND", priority = UserActivityLogging.Priority.CRITICAL, description = "멘토 2차 승인")
    public ResponseEntity<?> approveSecond(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserPrincipal loginUser) {
        adMentorRequestService.approveSecond(id, loginUser.getId());
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/request/{id}/request-approval")
    @PreAuthorize("hasAnyRole('MENTOR_C','MENTOR_B','MENTOR_A','ADMIN')")
    @UserActivityLogging(actionType = "MENTOR_REQUEST_APPROVAL", priority = UserActivityLogging.Priority.HIGH, description = "멘토 승인 요청")
    public ResponseEntity<?> requestApproval(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserPrincipal loginUser) {
        adMentorRequestService.requestApproval(id, loginUser.getId());
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/request/{id}/reject")
    @PreAuthorize("hasAnyRole('MENTOR_A','MENTOR_B','ADMIN')")
    @UserActivityLogging(actionType = "MENTOR_REQUEST_REJECT", priority = UserActivityLogging.Priority.CRITICAL, description = "멘토 신청 거부")
    public ResponseEntity<Void> rejectRequest(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserPrincipal loginUser,
            @Valid @RequestBody AdMentorRejectRequestDTO body) {
        adMentorRequestService.rejectRequest(id, loginUser.getId(), body.getReason());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/application")
    @PreAuthorize("hasAnyRole('USER', 'MENTOR_A', 'MENTOR_B', 'MENTOR_C', 'ADMIN')")
    @UserActivityLogging(actionType = "MENTOR_APPLICATION", scoreDelta = 20, priority = UserActivityLogging.Priority.HIGH, description = "멘토 신청")
    public ResponseEntity<ApiResponse<String>> applyMentor(
            @RequestBody @Valid MentorApplicationRequestDto requestDto,
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        mentorApplicationService.applyMentor(requestDto, userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success("멘토 신청이 완료되었습니다."));
    }

    @GetMapping("/application/status")
    @PreAuthorize("hasAnyRole('USER', 'MENTOR_A', 'MENTOR_B', 'MENTOR_C', 'ADMIN')")
    @UserActivityLogging(actionType = "MENTOR_APPLICATION_STATUS_VIEW", priority = UserActivityLogging.Priority.NORMAL, description = "멘토 신청 상태 조회")
    public ResponseEntity<ApiResponse<MentorApplicationResponseDto>> getMentorApplicationStatus(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        Optional<MentorApplicationResponseDto> status = mentorApplicationService
                .getMentorApplicationStatus(userPrincipal.getId());
        return status.map(dto -> ResponseEntity.ok(ApiResponse.success(dto)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/application/check")
    @PreAuthorize("hasAnyRole('USER', 'MENTOR_A', 'MENTOR_B', 'MENTOR_C', 'ADMIN')")
    @UserActivityLogging(actionType = "MENTOR_APPLICATION_CHECK", priority = UserActivityLogging.Priority.NORMAL, description = "멘토 신청 여부 확인")
    public ResponseEntity<ApiResponse<Boolean>> checkMentorApplication(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        boolean hasApplied = mentorApplicationService.hasAlreadyApplied(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(hasApplied));
    }

    @PostMapping("/application/presign")
    @PreAuthorize("hasAnyRole('USER', 'MENTOR_A', 'MENTOR_B', 'MENTOR_C', 'ADMIN')")
    @UserActivityLogging(actionType = "MENTOR_DOCUMENT_PRESIGN", priority = UserActivityLogging.Priority.NORMAL, description = "멘토 문서 업로드용 Presigned URL 생성")
    public ResponseEntity<ApiResponse<PresignedUrlResponse>> getMentorDocumentPresignedUrl(
            @RequestBody PresignedUrlRequest request) {
        String fileName = "mentor-documents/" + request.getFileName();
        String presignedUrl = presignedUrlService.generatePresignedUrl(fileName, request.getContentType());
        String publicUrl = presignedUrlService.getPublicUrl(fileName);
        return ResponseEntity.ok(ApiResponse.success(new PresignedUrlResponse(presignedUrl, publicUrl)));
    }

    // 🔐 인증된 사용자 (공지사항 열람)
    @GetMapping("/notices")
    @PreAuthorize("hasAnyRole('MENTOR_A', 'MENTOR_B', 'MENTOR_C', 'ADMIN')")
    @UserActivityLogging(actionType = "MENTOR_NOTICES_VIEW", priority = UserActivityLogging.Priority.NORMAL, description = "멘토 공지사항 목록 조회")
    public ResponseEntity<ApiResponse<org.springframework.data.domain.Page<MentorNoticeDto>>> getMentorNotices(
            @RequestParam(required = false) Long regionId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {

        boolean isAdmin = userPrincipal.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin) {
            var notices = mentorNoticeService.getNoticesByRegion(regionId, page, size, keyword);
            return ResponseEntity.ok(ApiResponse.success(notices));
        }

        List<Long> mentorRegionIds = adMentorUserRepository.findRegionIdByUserId(userPrincipal.getId());

        if (mentorRegionIds == null || mentorRegionIds.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success(org.springframework.data.domain.Page.empty()));
        }

        if (regionId != null && regionId != 99999L && !mentorRegionIds.contains(regionId)) {
            return ResponseEntity.ok(ApiResponse.success(org.springframework.data.domain.Page.empty()));
        }

        var notices = mentorNoticeService.getNoticesByRegionWithMentorFilter(regionId, mentorRegionIds, page, size,
                keyword);
        return ResponseEntity.ok(ApiResponse.success(notices));
    }

    @GetMapping("/notices/{noticeId}")
    @PreAuthorize("hasAnyRole('MENTOR_A', 'MENTOR_B', 'MENTOR_C', 'ADMIN')")
    @UserActivityLogging(actionType = "MENTOR_NOTICE_DETAIL_VIEW", priority = UserActivityLogging.Priority.NORMAL, description = "멘토 공지사항 상세 조회")
    public ResponseEntity<ApiResponse<MentorNoticeNavigationDto>> getMentorNoticeDetail(@PathVariable Long noticeId) {
        var notice = mentorNoticeService.getNoticeWithNavigation(noticeId);
        return ResponseEntity.ok(ApiResponse.success(notice));
    }

    // 멘토 권한 필요 (작성)
    @PostMapping("/notices")
    @PreAuthorize("hasAnyRole('MENTOR_A', 'MENTOR_B', 'MENTOR_C', 'ADMIN')")
    @UserActivityLogging(actionType = "MENTOR_NOTICE_CREATE", scoreDelta = 5, priority = UserActivityLogging.Priority.HIGH, description = "멘토 공지사항 작성")
    public ResponseEntity<ApiResponse<Long>> createMentorNotice(
            @RequestBody @Valid MentorNoticeCreateDto createDto,
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        Long noticeId = mentorNoticeService.createNotice(createDto, userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(noticeId));
    }

    // 멘토 권한 필요 (수정)
    @PutMapping("/notices/{noticeId}")
    @PreAuthorize("hasAnyRole('MENTOR_A', 'MENTOR_B', 'MENTOR_C', 'ADMIN')")
    @UserActivityLogging(actionType = "MENTOR_NOTICE_UPDATE", priority = UserActivityLogging.Priority.HIGH, description = "멘토 공지사항 수정")
    public ResponseEntity<ApiResponse<String>> updateMentorNotice(
            @PathVariable Long noticeId, 
            @RequestBody @Valid MentorNoticeCreateDto updateDto,
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        boolean isAdmin = userPrincipal.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        mentorNoticeService.updateNotice(noticeId, updateDto, userPrincipal.getId(), isAdmin);
        return ResponseEntity.ok(ApiResponse.success("공지사항이 수정되었습니다."));
    }

    // 멘토 또는 관리자 (삭제)
    @PostMapping("/notices/{noticeId}/delete")
    @PreAuthorize("hasAnyRole('MENTOR_A', 'MENTOR_B', 'MENTOR_C', 'ADMIN')")
    @UserActivityLogging(actionType = "MENTOR_NOTICE_DELETE", scoreDelta = -5, priority = UserActivityLogging.Priority.HIGH, description = "멘토 공지사항 삭제")
    public ResponseEntity<ApiResponse<String>> deleteMentorNotice(
            @PathVariable Long noticeId,
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        boolean isAdmin = userPrincipal.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        mentorNoticeService.deleteNotice(noticeId, userPrincipal.getId(), isAdmin);
        return ResponseEntity.ok(ApiResponse.success("공지사항이 삭제되었습니다."));
    }

    // 인증된 사용자 (최근 공지)
    @GetMapping("/notices/recent")
    @PreAuthorize("hasAnyRole('MENTOR_A', 'MENTOR_B', 'MENTOR_C', 'ADMIN')")
    @UserActivityLogging(actionType = "MENTOR_NOTICES_RECENT_VIEW", priority = UserActivityLogging.Priority.NORMAL, description = "최근 멘토 공지사항 조회")
    public ResponseEntity<ApiResponse<List<MentorNoticeDto>>> getRecentMentorNotices(
            @RequestParam Long regionId,
            @RequestParam(defaultValue = "5") int limit) {
        List<MentorNoticeDto> notices = mentorNoticeService.getRecentNotices(regionId, limit);
        return ResponseEntity.ok(ApiResponse.success(notices));
    }

    // 멘토 프로필 조회 API
    @GetMapping("/profile/me")
    @PreAuthorize("hasAnyRole('MENTOR_A', 'MENTOR_B', 'MENTOR_C', 'ADMIN')")
    @UserActivityLogging(actionType = "MENTOR_PROFILE_VIEW", priority = UserActivityLogging.Priority.NORMAL, description = "내 멘토 프로필 조회")
    public ResponseEntity<ApiResponse<AdMentorUserDTO>> getMyMentorProfile(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        AdMentorUserDTO mentorProfile = adMentorUserService.getMentorProfileByUserId(userPrincipal.getId());
        if (mentorProfile != null) {
            return ResponseEntity.ok(ApiResponse.success(mentorProfile));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // 해당 유저 프로
    @GetMapping("/profile/{userId}")
    @PreAuthorize("hasAnyRole('MENTOR_A', 'MENTOR_B', 'MENTOR_C', 'ADMIN')")
    @UserActivityLogging(actionType = "MENTOR_PROFILE_VIEW_BY_ID", priority = UserActivityLogging.Priority.NORMAL, description = "특정 멘토 프로필 조회")
    public ResponseEntity<ApiResponse<AdMentorUserDTO>> getMentorProfile(@PathVariable Long userId) {
        AdMentorUserDTO mentorProfile = adMentorUserService.getMentorProfileByUserId(userId);
        if (mentorProfile != null) {
            return ResponseEntity.ok(ApiResponse.success(mentorProfile));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // 멘토가 담당하는 지역 목록 조회 API
    @GetMapping("/regions/me")
    @PreAuthorize("hasAnyRole('MENTOR_A', 'MENTOR_B', 'MENTOR_C', 'ADMIN')")
    @UserActivityLogging(actionType = "MENTOR_REGIONS_VIEW", priority = UserActivityLogging.Priority.NORMAL, description = "내 담당 지역 목록 조회")
    public ResponseEntity<ApiResponse<List<Long>>> getMyMentorRegions(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        List<Long> mentorRegions = adMentorUserRepository.findRegionIdByUserId(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(mentorRegions != null ? mentorRegions : List.of()));
    }
    
    // 공지사항 수정/삭제 권한 체크
    @GetMapping("/notices/{noticeId}/permissions")
    @PreAuthorize("hasAnyRole('MENTOR_A', 'MENTOR_B', 'MENTOR_C', 'ADMIN')")
    @UserActivityLogging(actionType = "MENTOR_NOTICE_PERMISSION_CHECK", priority = UserActivityLogging.Priority.NORMAL, description = "멘토 공지사항 권한 확인")
    public ResponseEntity<ApiResponse<Boolean>> checkNoticePermission(
            @PathVariable Long noticeId,
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        boolean isAdmin = userPrincipal.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        boolean canEdit = mentorNoticeService.canEditOrDelete(noticeId, userPrincipal.getId(), isAdmin);
        return ResponseEntity.ok(ApiResponse.success(canEdit));
    }
}