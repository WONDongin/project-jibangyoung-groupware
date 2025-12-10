package com.jibangyoung.domain.mentor.service;

import com.jibangyoung.domain.mentor.dto.MentorNoticeCreateDto;
import com.jibangyoung.domain.mentor.dto.MentorNoticeDto;
import com.jibangyoung.domain.mentor.dto.MentorNoticeNavigationDto;
import com.jibangyoung.domain.mentor.entity.MentorNotice;
import com.jibangyoung.domain.mentor.repository.MentorNoticeRepository;
import com.jibangyoung.domain.policy.entity.Region;
import com.jibangyoung.domain.policy.repository.RegionRepository;
import com.jibangyoung.global.exception.BusinessException;
import com.jibangyoung.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MentorNoticeService {
    
    private final MentorNoticeRepository mentorNoticeRepository;
    private final RegionRepository regionRepository;
    
    // 임시 사용자 ID (관리자)
    private static final Long TEMP_ADMIN_ID = 1L;
    
    @Transactional
    public Long createNotice(MentorNoticeCreateDto createDto, Long authorId) {
        MentorNotice notice = createDto.toEntity(authorId);
        MentorNotice savedNotice = mentorNoticeRepository.save(notice);
        
        log.info("멘토 공지사항 생성 완료 - ID: {}, 제목: {}", savedNotice.getId(), savedNotice.getTitle());
        return savedNotice.getId();
    }
    
    @Transactional(readOnly = true)
    public Page<MentorNoticeDto> getNoticesByRegion(Long regionId, int page, int size, String keyword) {
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<MentorNotice> notices;
        
        if (regionId == null || regionId == 0) {
            // 전체 공지 조회
            if (keyword != null && !keyword.trim().isEmpty()) {
                notices = mentorNoticeRepository.findByTitleContainingOrderByCreatedAtDesc(keyword.trim(), pageable);
            } else {
                notices = mentorNoticeRepository.findAllOrderByCreatedAtDesc(pageable);
            }
        } else {
            // 특정 지역 공지 조회 + 전국 공지(99999) 포함
            if (keyword != null && !keyword.trim().isEmpty()) {
                notices = mentorNoticeRepository.findByRegionIdOrNationalWithKeywordOrderByCreatedAtDesc(
                        regionId, 99999L, keyword.trim(), pageable);
            } else {
                notices = mentorNoticeRepository.findByRegionIdOrNationalOrderByCreatedAtDesc(regionId, 99999L, pageable);
            }
        }
        
        return notices.map(notice -> {
            if (notice.getRegionId() == 99999) {
                return MentorNoticeDto.fromWithRegionInfo(notice, "99999", "전국");
            }
            
            Region region = regionRepository.findById(notice.getRegionId()).orElse(null);
            String regionCode = region != null ? String.valueOf(region.getRegionCode()) : "";
            String regionName = region != null ? region.getSido() + " " + region.getGuGun1() : "";
            return MentorNoticeDto.fromWithRegionInfo(notice, regionCode, regionName);
        });
    }

    @Transactional(readOnly = true)
    public Page<MentorNoticeDto> getNoticesByRegionWithMentorFilter(Long regionId, List<Long> mentorRegionIds, int page, int size, String keyword) {
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<MentorNotice> notices;
        boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();

        if (regionId != null && regionId != 0) { // 특정 지역 선택
            if (mentorRegionIds.contains(regionId) || regionId == 99999L) {
                if (hasKeyword) {
                    notices = mentorNoticeRepository.findByRegionIdAndTitleContainingOrderByCreatedAtDesc(
                            regionId, keyword.trim(), pageable);
                } else {
                    notices = mentorNoticeRepository.findByRegionIdOrderByCreatedAtDesc(regionId, pageable);
                }
            } else {
                notices = Page.empty(pageable);
            }
        } else { // 전체 지역 선택 (멘토의 담당 지역 + 전국)
            List<Long> allRegionIds = new ArrayList<>(mentorRegionIds);
            allRegionIds.add(99999L); // 전국 공지 포함

            if (hasKeyword) {
                notices = mentorNoticeRepository.findByRegionIdInWithKeywordOrderByCreatedAtDesc(
                        allRegionIds, keyword.trim(), pageable);
            } else {
                notices = mentorNoticeRepository.findByRegionIdInOrderByCreatedAtDesc(allRegionIds, pageable);
            }
        }
        
        return notices.map(notice -> {
            if (notice.getRegionId() == 99999) {
                return MentorNoticeDto.fromWithRegionInfo(notice, "99999", "전국");
            }
            
            Region region = regionRepository.findById(notice.getRegionId()).orElse(null);
            String regionCode = region != null ? String.valueOf(region.getRegionCode()) : "";
            String regionName = region != null ? region.getSido() + " " + region.getGuGun1() : "";
            return MentorNoticeDto.fromWithRegionInfo(notice, regionCode, regionName);
        });
    }
    
    @Transactional(readOnly = true)
    public MentorNoticeDto getNoticeDetail(Long noticeId) {
        MentorNotice notice = mentorNoticeRepository.findByIdWithAuthor(noticeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTICE_NOT_MENTOR));
                
        if (notice.getRegionId() == 99999) {
            return MentorNoticeDto.fromWithRegionInfo(notice, "99999", "전국");
        }
        
        Region region = regionRepository.findById(notice.getRegionId()).orElse(null);
        String regionCode = region != null ? String.valueOf(region.getRegionCode()) : "";
        String regionName = region != null ? region.getSido() + " " + region.getGuGun1() : "";
        
        return MentorNoticeDto.fromWithRegionInfo(notice, regionCode, regionName);
    }
    
    @Transactional(readOnly = true)
    public MentorNoticeNavigationDto getNoticeWithNavigation(Long noticeId) {
        // 현재 공지사항 조회
        MentorNotice notice = mentorNoticeRepository.findByIdWithAuthor(noticeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTICE_NOT_MENTOR));
                
        MentorNoticeDto currentNotice;
        if (notice.getRegionId() == 99999) {
            currentNotice = MentorNoticeDto.fromWithRegionInfo(notice, "99999", "전국");
        } else {
            Region region = regionRepository.findById(notice.getRegionId()).orElse(null);
            String regionCode = region != null ? String.valueOf(region.getRegionCode()) : "";
            String regionName = region != null ? region.getSido() + " " + region.getGuGun1() : "";
            currentNotice = MentorNoticeDto.fromWithRegionInfo(notice, regionCode, regionName);
        }
        
        // 이전 글 조회
        Pageable pageable = PageRequest.of(0, 1);
        List<MentorNotice> previousList = mentorNoticeRepository.findPreviousNotice(noticeId, pageable);
        MentorNoticeNavigationDto.NavigationItem previous = previousList.isEmpty() ? null : 
                MentorNoticeNavigationDto.NavigationItem.builder()
                        .id(previousList.get(0).getId())
                        .title(previousList.get(0).getTitle())
                        .build();
        
        // 다음 글 조회
        List<MentorNotice> nextList = mentorNoticeRepository.findNextNotice(noticeId, pageable);
        MentorNoticeNavigationDto.NavigationItem next = nextList.isEmpty() ? null :
                MentorNoticeNavigationDto.NavigationItem.builder()
                        .id(nextList.get(0).getId())
                        .title(nextList.get(0).getTitle())
                        .build();
        
        return MentorNoticeNavigationDto.builder()
                .current(currentNotice)
                .previous(previous)
                .next(next)
                .build();
    }
    
    @Transactional(readOnly = true)
    public List<MentorNoticeDto> getRecentNotices(Long regionId, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<MentorNotice> notices = mentorNoticeRepository.findTop5ByRegionIdOrderByCreatedAtDesc(regionId, pageable);
        
        return notices.stream()
                .map(notice -> {
                    if (notice.getRegionId() == 99999) {
                        return MentorNoticeDto.fromWithRegionInfo(notice, "99999", "전국");
                    }
                    
                    Region region = regionRepository.findById(notice.getRegionId()).orElse(null);
                    String regionCode = region != null ? String.valueOf(region.getRegionCode()) : "";
                    String regionName = region != null ? region.getSido() + " " + region.getGuGun1() : "";
                    return MentorNoticeDto.fromWithRegionInfo(notice, regionCode, regionName);
                })
                .collect(Collectors.toList());
    }
    
    @Transactional
    public void updateNotice(Long noticeId, MentorNoticeCreateDto updateDto, Long userId, boolean isAdmin) {
        MentorNotice notice = mentorNoticeRepository.findByIdWithAuthor(noticeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTICE_NOT_MENTOR));
        
        // 권한 체크: 작성자 본인이거나 관리자만 수정 가능
        if (!isAdmin && !notice.getAuthorId().equals(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
                
        // 제목과 내용, 지역 업데이트
        notice.setTitle(updateDto.getTitle());
        notice.setContent(updateDto.getContent());
        notice.setRegionId(updateDto.getRegionId());
        if (updateDto.getFileUrl() != null) {
            notice.setFileUrl(updateDto.getFileUrl());
        }
        
        mentorNoticeRepository.save(notice);
        log.info("멘토 공지사항 수정 완료 - ID: {}, 제목: {}", noticeId, updateDto.getTitle());
    }
    
    @Transactional
    public void deleteNotice(Long noticeId, Long userId, boolean isAdmin) {
        MentorNotice notice = mentorNoticeRepository.findByIdWithAuthor(noticeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTICE_NOT_MENTOR));
        
        // 권한 체크: 작성자 본인이거나 관리자만 삭제 가능
        if (!isAdmin && !notice.getAuthorId().equals(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
                
        mentorNoticeRepository.delete(notice);
        log.info("멘토 공지사항 삭제 완료 - ID: {}", noticeId);
    }
    
    @Transactional(readOnly = true)
    public boolean canEditOrDelete(Long noticeId, Long userId, boolean isAdmin) {
        MentorNotice notice = mentorNoticeRepository.findByIdWithAuthor(noticeId)
                .orElse(null);
        if (notice == null) {
            return false;
        }
        
        // 관리자이거나 작성자 본인인 경우 수정/삭제 가능
        return isAdmin || notice.getAuthorId().equals(userId);
    }
}