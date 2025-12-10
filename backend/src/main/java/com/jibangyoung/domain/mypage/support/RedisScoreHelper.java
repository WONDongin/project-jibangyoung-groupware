package com.jibangyoung.domain.mypage.support;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.jibangyoung.domain.mypage.dto.MyRegionScoreDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Redis 기반 점수 관리 헬퍼 클래스
 * - 실시간 점수 캐싱
 * - 배치 동기화를 위한 변경사항 추적
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisScoreHelper {

    private final RedisTemplate<String, Object> redisTemplate;

    // Redis 키 패턴
    private static final String USER_REGION_SCORE_KEY = "user_region_score:{}:{}"; // userId:regionId
    private static final String CHANGED_SCORES_KEY = "changed_scores"; // Set으로 변경된 항목 추적
    private static final String USER_SCORES_KEY = "user_scores:{}"; // userId별 모든 지역 점수

    /**
     * 사용자의 특정 지역 점수 조회
     */
    public MyRegionScoreDto getUserRegionScore(Long userId, Long regionId) {
        String key = USER_REGION_SCORE_KEY.replace("{}", String.valueOf(userId))
                .replace("{}", String.valueOf(regionId));

        Object score = redisTemplate.opsForValue().get(key);
        if (score != null) {
            return new MyRegionScoreDto(regionId.intValue(), Integer.parseInt(score.toString()));
        }
        return null;
    }

    /**
     * 사용자의 모든 지역별 점수 조회
     */
    public List<MyRegionScoreDto> getUserRegionScores(Long userId) {
        String pattern = "user_region_score:" + userId + ":*";
        return redisTemplate.keys(pattern).stream()
                .map(key -> {
                    String[] parts = key.split(":");
                    int regionId = Integer.parseInt(parts[2]);
                    Object score = redisTemplate.opsForValue().get(key);
                    return new MyRegionScoreDto(regionId,
                            score != null ? Integer.parseInt(score.toString()) : 0);
                })
                .collect(Collectors.toList());
    }

    /**
     * 사용자 지역 점수 업데이트
     */
    public void updateUserRegionScore(Long userId, Long regionId, int scoreDelta) {
        String key = USER_REGION_SCORE_KEY.replace("{}", String.valueOf(userId))
                .replace("{}", String.valueOf(regionId));

        // 현재 점수 조회 후 증감
        Object currentScore = redisTemplate.opsForValue().get(key);
        int newScore = (currentScore != null ? Integer.parseInt(currentScore.toString()) : 0) + scoreDelta;

        // 점수 업데이트
        redisTemplate.opsForValue().set(key, newScore);

        // 변경사항 추적에 추가
        redisTemplate.opsForSet().add(CHANGED_SCORES_KEY, key);

        log.debug("Redis 점수 업데이트 - userId: {}, regionId: {}, 변경량: {}, 새 점수: {}",
                userId, regionId, scoreDelta, newScore);
    }

    /**
     * 변경된 모든 점수 조회 (배치 동기화용)
     */
    public List<MyRegionScoreDto> getAllChangedScores() {
        return redisTemplate.opsForSet().members(CHANGED_SCORES_KEY).stream()
                .map(key -> {
                    String keyStr = key.toString();
                    String[] parts = keyStr.split(":");
                    if (parts.length >= 3) {
                        int regionId = Integer.parseInt(parts[2]);
                        Object score = redisTemplate.opsForValue().get(keyStr);
                        return new MyRegionScoreDto(regionId,
                                score != null ? Integer.parseInt(score.toString()) : 0);
                    }
                    return null;
                })
                .filter(dto -> dto != null)
                .collect(Collectors.toList());
    }

    /**
     * 처리된 점수 변경사항 정리
     */
    public void clearProcessedScores(List<MyRegionScoreDto> processedScores) {
        processedScores.forEach(score -> {
            // userId 추출이 필요한데, MyRegionScoreDto에는 regionId만 있음
            // 실제 구현에서는 userId 정보도 함께 저장하거나 다른 방식 필요
            String pattern = "*:" + score.regionId();
            redisTemplate.keys("user_region_score" + pattern).forEach(key -> {
                redisTemplate.opsForSet().remove(CHANGED_SCORES_KEY, key);
            });
        });
    }

    /**
     * 점수에 해당하는 userId 조회 (임시 구현)
     * 실제로는 Redis 구조를 개선하거나 다른 방식으로 userId 추적 필요
     */
    public Long getUserIdForScore(int regionId) {
        String pattern = "user_region_score:*:" + regionId;
        return redisTemplate.keys(pattern).stream()
                .findFirst()
                .map(key -> {
                    String[] parts = key.split(":");
                    return Long.parseLong(parts[1]);
                })
                .orElse(null);
    }

    /**
     * 특정 사용자의 Redis 점수 데이터 초기화
     */
    public void initializeUserScores(Long userId, List<MyRegionScoreDto> scores) {
        scores.forEach(score -> {
            String key = USER_REGION_SCORE_KEY.replace("{}", String.valueOf(userId))
                    .replace("{}", String.valueOf(score.regionId()));
            redisTemplate.opsForValue().set(key, score.score());
        });

        log.debug("사용자 점수 Redis 초기화 완료 - userId: {}, 점수 항목: {}", userId, scores.size());
    }

    /**
     * 사용자의 모든 Redis 점수 데이터 삭제
     */
    public void clearUserScores(Long userId) {
        String pattern = "user_region_score:" + userId + ":*";
        redisTemplate.keys(pattern).forEach(redisTemplate::delete);

        log.debug("사용자 Redis 점수 데이터 삭제 완료 - userId: {}", userId);
    }
}