package com.jibangyoung.domain.mentor.service;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;

import com.jibangyoung.domain.mentor.dto.AdMentorUserDTO;
import com.jibangyoung.domain.mentor.repository.AdMentorUserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdMentorUserService {

    private final AdMentorUserRepository adMentorUserRepository;

    // 멘토 대시보드_전체 멘토 리스트(admin)
    public List<AdMentorUserDTO> getAllMentorUsers() {
        return adMentorUserRepository.findAllMentorUsers();
    }

    // 멘토 대시보드_내 지역 멘토 리스트
    public List<AdMentorUserDTO> getAdMentorId(Long mentorId) {
        // mentorId로 regionId 조회
        List<Long> regionIds = adMentorUserRepository.findRegionIdByUserId(mentorId);
        if (regionIds == null || regionIds.isEmpty()) {
            return Collections.emptyList();
        }
        // regionId로 해당 멘토 유저들 조회
        return adMentorUserRepository.findUsersByMentorRegionIds(regionIds);
    }

    // 특정 사용자의 멘토 프로필 조회 (여러 지역 담당 가능)
    public AdMentorUserDTO getMentorProfileByUserId(Long userId) {
        List<Long> regionIds = adMentorUserRepository.findRegionIdByUserId(userId);
        if (regionIds == null || regionIds.isEmpty()) {
            return null;
        }
        
        // 해당 사용자의 멘토 정보를 반환
        List<AdMentorUserDTO> mentorProfiles = adMentorUserRepository.findUsersByMentorRegionIds(regionIds);
        return mentorProfiles.stream()
                .filter(profile -> profile.getId().equals(userId))
                .findFirst()
                .orElse(null);
    }
}
