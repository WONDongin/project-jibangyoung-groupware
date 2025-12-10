package com.jibangyoung.domain.mypage.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jibangyoung.domain.mypage.dto.ActivityEventDto;
import com.jibangyoung.domain.mypage.dto.MyRegionScoreDto;
import com.jibangyoung.domain.mypage.dto.RegionScoreDto;
import com.jibangyoung.domain.mypage.dto.RegionScoreDto.ScoreHistoryItem;
import com.jibangyoung.domain.mypage.entity.UserActivityEvent;
import com.jibangyoung.domain.mypage.entity.UserRegionScore;
import com.jibangyoung.domain.mypage.repository.UserActivityEventRepository;
import com.jibangyoung.domain.mypage.repository.UserRegionScoreRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ScoreService {

    private final UserActivityEventRepository activityRepo;
    private final UserRegionScoreRepository scoreRepo;
    private final UserRegionScoreRepository userRegionScoreRepository;

    @Transactional(readOnly = true)
    public RegionScoreDto getRegionScore(Long userId, Long regionId) {
        // 점수 엔티티 조회
        UserRegionScore score = scoreRepo.findByUserIdAndRegionId(userId, regionId)
                .orElse(new UserRegionScore(userId, regionId, 0L, 0, 0, null));

        int postCount = score.getPostCount();
        int commentCount = score.getCommentCount();
        int mentoringCount = getMentoringCount(userId, regionId); // 실제 구현 필요

        int totalScore = score.getTotalScore() == null ? 0 : score.getTotalScore().intValue();
        double progress = Math.min(1.0, totalScore / 100.0);
        int daysToMentor = 7;

        List<ScoreHistoryItem> history = activityRepo
                .findTop30ByUserIdAndRegionIdOrderByCreatedAtDesc(userId, regionId)
                .stream()
                .map(ev -> new ScoreHistoryItem(
                        ev.getCreatedAt() != null ? ev.getCreatedAt().toLocalDate().toString() : null,
                        ev.getScoreDelta() == null ? 0 : ev.getScoreDelta(),
                        ev.getActionType() == null ? "" : ev.getActionType()))
                .collect(Collectors.toList());

        String regionName = getRegionName(regionId);

        return new RegionScoreDto(
                regionId.intValue(), regionName, postCount, commentCount, mentoringCount,
                totalScore, progress, daysToMentor, history);
    }

    @Transactional
    public void recordUserActivity(ActivityEventDto dto) {
        // UserActivityEvent 엔티티 생성 및 저장 (예시, 실제 필드 매칭 필요)
        UserActivityEvent event = UserActivityEvent.builder()
                .userId(dto.userId())
                .regionId(dto.regionId())
                .actionType(dto.actionType())
                .refId(dto.refId())
                .parentRefId(dto.parentRefId())
                .actionValue(dto.actionValue())
                .scoreDelta(dto.scoreDelta())
                .meta(dto.meta())
                .ipAddr(dto.ipAddr())
                .userAgent(dto.userAgent())
                .platform(dto.platform())
                .lang(dto.lang())
                .status(dto.status())
                .memo(dto.memo())
                .createdAt(dto.createdAt())
                .updatedAt(dto.updatedAt())
                .build();
        activityRepo.save(event);

        // 실시간 점수 업데이트 - regionId를 Long으로 변환
        updateUserRegionScore(dto.userId(), Long.valueOf(dto.regionId()), dto.actionType(), dto.scoreDelta());
    }

    @Transactional(readOnly = true)
    public List<RegionScoreDto> getTopRankByRegion(Long regionId, int size) {
        List<UserRegionScore> rank = scoreRepo.findByRegionIdOrderByTotalScoreDesc(regionId);
        return rank.stream()
                .limit(size)
                .map(rs -> new RegionScoreDto(
                        rs.getRegionId().intValue(),
                        getRegionName(rs.getRegionId()),
                        rs.getPostCount(),
                        rs.getCommentCount(),
                        0, // mentoringCount는 별도 계산
                        rs.getTotalScore() == null ? 0 : rs.getTotalScore().intValue(),
                        0.0, 0,
                        List.of()))
                .collect(Collectors.toList());
    }

    // ✅ [추가] 내 모든 지역별 점수 조회
    public List<MyRegionScoreDto> getUserRegionScores(Long userId) {
        return userRegionScoreRepository.findByUserId(userId)
                .stream()
                .map(entity -> new MyRegionScoreDto(entity.getRegionId().intValue(), entity.getTotalScore().intValue()))
                .collect(Collectors.toList());
    }

    // ✅ [추가] 사용자 지역 점수 업데이트
    @Transactional
    public void updateUserRegionScore(Long userId, Long regionId, String actionType, Integer scoreDelta) {
        UserRegionScore userScore = scoreRepo.findByUserIdAndRegionId(userId, regionId)
                .orElse(UserRegionScore.builder()
                        .userId(userId)
                        .regionId(regionId)
                        .totalScore(0L)
                        .postCount(0)
                        .commentCount(0)
                        .build());

        if (scoreDelta != null) {
            userScore.recordActivity(actionType, scoreDelta);
        }

        scoreRepo.save(userScore);
    }

    // ---- 실제 쿼리 구현 필요 (임시 더미) ----
    private int getMentoringCount(Long userId, Long regionId) {
        return 0;
    }

    // ---- regionId → regionName 매핑 ----
    private String getRegionName(Long regionId) {
        return switch (regionId.intValue()) {
            case 30123 -> "경기";
            case 30124 -> "서울";
            case 30125 -> "강원";
            case 30126 -> "부산";
            case 30127 -> "대전";
            case 30128 -> "제주";
            default -> "기타";
        };
    }
}