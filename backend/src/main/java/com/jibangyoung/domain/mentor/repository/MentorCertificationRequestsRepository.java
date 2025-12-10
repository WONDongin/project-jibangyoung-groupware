package com.jibangyoung.domain.mentor.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.jibangyoung.domain.mentor.dto.AdMentorRequestDTO;
import com.jibangyoung.domain.mentor.entity.MentorCertificationRequests;

@Repository
public interface MentorCertificationRequestsRepository extends JpaRepository<MentorCertificationRequests, Long> {
    
    // 사용자별 멘토 신청 상태 조회
    Optional<MentorCertificationRequests> findByUserId(Long userId);
    
    // 사용자가 이미 신청했는지 확인
    boolean existsByUserId(Long userId);
    
    // 사용자가 특정 지역에 이미 신청했는지 확인
    boolean existsByUserIdAndRegionId(Long userId, Long regionId);

    // 멘토_데시보드_멘토신청 리스트
    @Query("""
        SELECT new com.jibangyoung.domain.mentor.dto.AdMentorRequestDTO(
            r.id,
            r.userId,
            r.userName,
            u.nickname,                
            r.userEmail,
            r.regionId,
            r.reason,
            r.governmentAgency,
            r.documentUrl,
            CAST(r.status AS string),
            r.createdAt,
            r.reviewedAt,
            r.reviewedBy,
            r.rejectionReason
        )
        FROM MentorCertificationRequests r
        LEFT JOIN User u ON r.reviewedBy = u.id
        WHERE r.regionId IN :regionIds
    """)
    List<AdMentorRequestDTO> findByRegionIdsWithReviewerNickname(@Param("regionIds") List<Long> regionIds);
}