package com.jibangyoung.domain.mypage.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jibangyoung.domain.mypage.entity.UserRegionScore;
import com.jibangyoung.domain.mypage.entity.UserRegionScoreId;

// UserRegionScoreRepository.java (Long 타입으로 수정)
public interface UserRegionScoreRepository extends JpaRepository<UserRegionScore, UserRegionScoreId> {
    List<UserRegionScore> findByUserId(Long userId);

    List<UserRegionScore> findByRegionIdOrderByTotalScoreDesc(Long regionId);

    Optional<UserRegionScore> findByUserIdAndRegionId(Long userId, Long regionId);

    // 예시: 특정 사용자의 특정 지역 점수 존재 여부 확인
    boolean existsByUserIdAndRegionId(Long userId, Long regionId);

    // 예시: 특정 사용자의 지역별 점수 합계
    // @Query("SELECT SUM(u.totalScore) FROM UserRegionScore u WHERE u.userId =
    // :userId")
    // Long getTotalScoreByUserId(@Param("userId") Long userId);
}