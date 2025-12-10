package com.jibangyoung.global.batch;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

import com.jibangyoung.global.common.UserActivityLog;
import com.jibangyoung.global.repository.UserActivityLogRedisRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 📖 Redis에서 로그 읽기 (수정된 버전)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserActivityLogReader implements ItemReader<UserActivityLog> {

    private final UserActivityLogRedisRepository redisRepository;
    private final Queue<UserActivityLog> logQueue = new ConcurrentLinkedQueue<>();
    private static final int BATCH_SIZE = 50; // 배치 사이즈 줄임
    private boolean endOfData = false;
    private int readCount = 0;

    @Override
    public UserActivityLog read() throws Exception {
        // 큐가 비어있고 더 데이터가 있을 수 있으면 로드
        if (logQueue.isEmpty() && !endOfData) {
            loadLogsFromRedis();
        }

        UserActivityLog activityLog = logQueue.poll();

        if (activityLog != null) {
            readCount++;
            log.info("📖 로그 읽기 [{}]: logId={}, actionType={}, userId={}",
                    readCount, activityLog.getLogId(), activityLog.getActionType(), activityLog.getUserId());
        } else {
            log.info("📖 Reader 완료: 총 {}개 로그 읽음", readCount);
        }

        return activityLog;
    }

    private void loadLogsFromRedis() {
        try {
            log.info("📖 Redis에서 로그 로드 시작... (배치크기: {})", BATCH_SIZE);

            List<String> logIds = redisRepository.getLogsForBatch(BATCH_SIZE);

            if (logIds.isEmpty()) {
                log.info("📖 Redis에서 로드할 로그가 없음 - 데이터 종료");
                endOfData = true;
                return;
            }

            int loadedCount = 0;

            for (String logId : logIds) {
                try {
                    UserActivityLog activityLog = redisRepository.getLog(logId);
                    if (activityLog != null) {
                        logQueue.offer(activityLog);
                        loadedCount++;
                    } else {
                        log.warn("📖 로그 로드 실패 (null): logId={}", logId);
                    }
                } catch (Exception e) {
                    log.error("📖 로그 로드 오류: logId={}", logId, e);
                }
            }

            log.info("📖 Redis 로그 로드 완료: 로드={}개, 큐크기={}", loadedCount, logQueue.size());

            // 로드된 데이터가 배치 크기보다 적으면 더 이상 데이터가 없을 가능성
            if (loadedCount < BATCH_SIZE) {
                endOfData = true;
            }

        } catch (Exception e) {
            log.error("📖 Redis 로그 로드 실패", e);
            endOfData = true;
        }
    }

    /**
     * Reader 상태 리셋
     */
    public void reset() {
        logQueue.clear();
        endOfData = false;
        readCount = 0;
        log.info("📖 Reader 상태 리셋 완료");
    }
}