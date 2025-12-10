package com.jibangyoung.domain.mypage.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@IdClass(UserRegionScoreId.class)
@Table(name = "user_region_score")
public class UserRegionScore {

    @Id
    private Long userId;

    @Id
    private Long regionId;

    @Column(nullable = false)
    private Long totalScore; // 전체 점수 합계

    @Column(nullable = false)
    private Integer postCount; // 게시글 작성 수

    @Column(nullable = false)
    private Integer commentCount; // 댓글 작성 수

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public UserRegionScore(Long userId,
            Long regionId,
            Long totalScore,
            Integer postCount,
            Integer commentCount,
            LocalDateTime updatedAt) {
        this.userId = userId;
        this.regionId = regionId;
        this.totalScore = totalScore == null ? 0L : totalScore;
        this.postCount = postCount == null ? 0 : postCount;
        this.commentCount = commentCount == null ? 0 : commentCount;
        this.updatedAt = updatedAt == null ? LocalDateTime.now() : updatedAt;
    }

    /** 점수 누적 */
    public void addScore(long delta) {
        this.totalScore += delta;
        this.updatedAt = LocalDateTime.now();
    }

    /** 게시글 수 증가 */
    public void incrementPostCount() {
        this.postCount++;
        this.updatedAt = LocalDateTime.now();
    }

    /** 댓글 수 증가 */
    public void incrementCommentCount() {
        this.commentCount++;
        this.updatedAt = LocalDateTime.now();
    }

    /** 점수/횟수 일괄 반영 */
    public void recordActivity(String actionType, long scoreDelta) {
        this.totalScore += scoreDelta;
        if ("POST".equalsIgnoreCase(actionType)) {
            this.postCount++;
        } else if ("COMMENT".equalsIgnoreCase(actionType)) {
            this.commentCount++;
        }
        this.updatedAt = LocalDateTime.now();
    }
}
