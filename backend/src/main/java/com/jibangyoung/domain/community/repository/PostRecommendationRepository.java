package com.jibangyoung.domain.community.repository;

import com.jibangyoung.domain.community.entity.PostRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface PostRecommendationRepository extends JpaRepository<PostRecommendation, Long> {
    Optional<PostRecommendation> findByUserIdAndPostId(Long userId, Long postId);
    
    // 게시글의 각 추천 유형별 개수 조회
    @Query("SELECT pr.recommendationType, COUNT(pr) FROM PostRecommendation pr WHERE pr.post.id = :postId GROUP BY pr.recommendationType")
    List<Object[]> countRecommendationsByPostIdGroupByType(@Param("postId") Long postId);
}
