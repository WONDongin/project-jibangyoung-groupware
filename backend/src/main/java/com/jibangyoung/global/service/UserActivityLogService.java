package com.jibangyoung.global.service;

import java.util.concurrent.CompletableFuture;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.jibangyoung.global.common.UserActivityLog;
import com.jibangyoung.global.repository.UserActivityLogRedisRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 🚀 사용자 활동 로그 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserActivityLogService {

    private final UserActivityLogRedisRepository redisRepository;

    @Async("logTaskExecutor")
    public CompletableFuture<Void> saveLogAsync(UserActivityLog activityLog) {
        try {
            redisRepository.saveLog(activityLog);
            log.debug("비동기 로그 저장 완료: logId={}, actionType={}",
                    activityLog.getLogId(), activityLog.getActionType());
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("비동기 로그 저장 실패: logId={}", activityLog.getLogId(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    public void saveLogSync(UserActivityLog activityLog) {
        try {
            redisRepository.saveLog(activityLog);
            log.debug("동기 로그 저장 완료: logId={}", activityLog.getLogId());
        } catch (Exception e) {
            log.error("동기 로그 저장 실패: logId={}", activityLog.getLogId(), e);
        }
    }

    public long getQueueSize() {
        return redisRepository.getBatchQueueSize();
    }
}