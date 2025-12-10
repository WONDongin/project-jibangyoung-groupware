package com.jibangyoung.domain.mypage.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jibangyoung.domain.mypage.entity.UserActivityEvent;

public interface UserActivityEventRepository extends JpaRepository<UserActivityEvent, Long> {

    // regionId 타입을 Long으로 수정
    List<UserActivityEvent> findTop30ByUserIdAndRegionIdOrderByCreatedAtDesc(Long userId, Long regionId);

    // 추가적인 쿼리 메서드들
    List<UserActivityEvent> findByUserIdAndRegionIdOrderByCreatedAtDesc(Long userId, Long regionId);

    List<UserActivityEvent> findByUserIdOrderByCreatedAtDesc(Long userId);

    // 특정 액션 타입으로 필터링
    List<UserActivityEvent> findByUserIdAndActionTypeOrderByCreatedAtDesc(Long userId, String actionType);
}