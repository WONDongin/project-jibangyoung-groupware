package com.jibangyoung.domain.mentor.service;

import com.jibangyoung.domain.auth.entity.User;
import com.jibangyoung.domain.auth.repository.UserRepository;
import com.jibangyoung.domain.mentor.dto.MentorApplicationRequestDto;
import com.jibangyoung.domain.mentor.dto.MentorApplicationResponseDto;
import com.jibangyoung.domain.mentor.entity.MentorCertificationRequests;
import com.jibangyoung.domain.mentor.repository.MentorCertificationRequestsRepository;
import com.jibangyoung.global.exception.BusinessException;
import com.jibangyoung.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MentorApplicationService {
    
    private final MentorCertificationRequestsRepository mentorRequestRepository;
    private final UserRepository userRepository;

    @Transactional
    public void applyMentor(MentorApplicationRequestDto requestDto, Long userId) {
        // 사용자 정보 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 같은 지역에 이미 신청했는지 확인
        if (mentorRequestRepository.existsByUserIdAndRegionId(userId, requestDto.getRegionId())) {
            throw new BusinessException(ErrorCode.ALREADY_APPLIED_MENTOR, "이미 해당 지역에 멘토 신청을 하셨습니다.");
        }

        // 멘토 신청 엔티티 생성 및 저장
        MentorCertificationRequests application = requestDto.toEntity(
                userId, 
                user.getUsername(), 
                user.getEmail()
        );
        
        mentorRequestRepository.save(application);
        
        log.info("멘토 신청 완료 - 사용자 ID: {}, 지역 ID: {}, 사용자명: {}", userId, requestDto.getRegionId(), user.getUsername());
    }


    @Transactional(readOnly = true)
    public Optional<MentorApplicationResponseDto> getMentorApplicationStatus(Long userId) {
        return mentorRequestRepository.findByUserId(userId)
                .map(MentorApplicationResponseDto::from);
    }
    
    @Transactional(readOnly = true)
    public boolean hasAlreadyApplied(Long userId) {
        return mentorRequestRepository.existsByUserId(userId);
    }
}