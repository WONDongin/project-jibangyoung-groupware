package com.jibangyoung.domain.mypage.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jibangyoung.domain.mypage.dto.MyRegionScoreDto;
import com.jibangyoung.domain.mypage.entity.UserRegionScore;
import com.jibangyoung.domain.mypage.repository.UserRegionScoreRepository;
import com.jibangyoung.domain.mypage.support.RedisScoreHelper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlushScoreService {

    private final RedisScoreHelper redisScoreHelper;
    private final UserRegionScoreRepository userRegionScoreRepository;

    /**
     * Redis → MariaDB 점수 동기화
     * 스케줄러에서 호출되는 배치 작업
     */
    @Transactional
    public void flushAllScoresToDB() {
        try {
            log.info("Redis → DB 점수 동기화 시작");

            // Redis에서 모든 변경된 점수 데이터 조회
            List<MyRegionScoreDto> allScores = redisScoreHelper.getAllChangedScores();

            if (allScores.isEmpty()) {
                log.info("동기화할 점수 변경사항이 없습니다.");
                return;
            }

            int processedCount = 0;
            for (MyRegionScoreDto scoreDto : allScores) {
                try {
                    flushSingleScore(scoreDto);
                    processedCount++;
                } catch (Exception e) {
                    log.error("점수 동기화 실패 - userId: {}, regionId: {}",
                            scoreDto.regionId(), scoreDto.score(), e);
                }
            }

            // Redis에서 처리된 항목들 정리
            redisScoreHelper.clearProcessedScores(allScores);

            log.info("Redis → DB 점수 동기화 완료. 처리된 항목: {}/{}", processedCount, allScores.size());

        } catch (Exception e) {
            log.error("점수 동기화 배치 작업 실패", e);
            throw e;
        }
    }

    /**
     * 단일 점수 항목 동기화
     */
    private void flushSingleScore(MyRegionScoreDto scoreDto) {
        // MyRegionScoreDto에서 userId 추출이 필요한데, 현재 구조상 regionId와 score만 있음
        // Redis에서 userId 정보도 함께 가져오도록 RedisScoreHelper 수정이 필요하거나
        // 다른 방식으로 userId를 식별해야 함

        // 임시로 Redis에서 userId를 함께 가져온다고 가정
        Long userId = redisScoreHelper.getUserIdForScore(scoreDto.regionId());
        Long regionId = Long.valueOf(scoreDto.regionId());

        if (userId == null) {
            log.warn("userId를 찾을 수 없음 - regionId: {}", scoreDto.regionId());
            return;
        }

        UserRegionScore existingScore = userRegionScoreRepository
                .findByUserIdAndRegionId(userId, regionId)
                .orElse(null);

        if (existingScore == null) {
            // 새로운 점수 기록 생성
            UserRegionScore newScore = UserRegionScore.builder()
                    .userId(userId)
                    .regionId(regionId)
                    .totalScore(Long.valueOf(scoreDto.score()))
                    .postCount(0) // Redis에서 별도 관리하거나 계산 필요
                    .commentCount(0) // Redis에서 별도 관리하거나 계산 필요
                    .updatedAt(LocalDateTime.now())
                    .build();

            userRegionScoreRepository.save(newScore);
            log.debug("새 점수 기록 생성 - userId: {}, regionId: {}, score: {}",
                    userId, regionId, scoreDto.score());
        } else {
            // 기존 점수 업데이트
            long scoreDelta = scoreDto.score() - existingScore.getTotalScore();
            existingScore.addScore(scoreDelta);

            userRegionScoreRepository.save(existingScore);
            log.debug("점수 업데이트 - userId: {}, regionId: {}, 변경량: {}",
                    userId, regionId, scoreDelta);
        }
    }

    /**
     * 특정 사용자의 점수만 동기화 (실시간 동기화용)
     */
    @Transactional
    public void flushUserScore(Long userId, Long regionId) {
        try {
            MyRegionScoreDto redisScore = redisScoreHelper.getUserRegionScore(userId, regionId);
            if (redisScore != null) {
                flushSingleScore(redisScore);
                log.debug("사용자 점수 실시간 동기화 완료 - userId: {}, regionId: {}", userId, regionId);
            }
        } catch (Exception e) {
            log.error("사용자 점수 실시간 동기화 실패 - userId: {}, regionId: {}", userId, regionId, e);
            throw e;
        }
    }
}