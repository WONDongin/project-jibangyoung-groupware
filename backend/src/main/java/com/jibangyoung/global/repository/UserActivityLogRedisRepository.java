package com.jibangyoung.global.repository;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jibangyoung.global.common.UserActivityLog;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Repository
@RequiredArgsConstructor
@Slf4j
public class UserActivityLogRedisRepository {

    @Qualifier("userActivityLogRedisTemplate")
    private final RedisTemplate<String, UserActivityLog> userLogTemplate;

    @Qualifier("stringRedisTemplate")
    private final StringRedisTemplate stringTemplate;

    @Qualifier("redisTemplate")
    private final RedisTemplate<String, Object> legacyObjectTemplate;

    private final ObjectMapper objectMapper;

    private static final String LOG_KEY_PREFIX = "user_activity_log:";
    private static final String BATCH_QUEUE_KEY = "user_activity_log:batch_queue";
    private static final String PRIORITY_QUEUE_PREFIX = "user_activity_log:priority:";
    private static final long DEFAULT_TTL_HOURS = 24;

    public void saveLog(UserActivityLog activityLog) {
        try {
            String key = LOG_KEY_PREFIX + activityLog.getLogId();

            // 저장 전 로그 확인
            log.debug("Redis 로그 저장 시작: logId={}, userId={}, actionType={}",
                    activityLog.getLogId(), activityLog.getUserId(), activityLog.getActionType());

            // value 저장: 전용 템플릿으로 강타입 직렬화
            userLogTemplate.opsForValue().set(key, activityLog, DEFAULT_TTL_HOURS, TimeUnit.HOURS);

            // 큐 관리: 문자열 템플릿
            stringTemplate.opsForList().leftPush(BATCH_QUEUE_KEY, activityLog.getLogId());

            String priority = activityLog.getPriority() != null ? activityLog.getPriority().toLowerCase() : "normal";
            String priorityKey = PRIORITY_QUEUE_PREFIX + priority;
            stringTemplate.opsForZSet().add(priorityKey, activityLog.getLogId(), System.currentTimeMillis());

            log.debug("Redis 로그 저장 완료: logId={}, actionType={}, userId={}, 큐크기={}",
                    activityLog.getLogId(), activityLog.getActionType(), activityLog.getUserId(), getBatchQueueSize());

        } catch (Exception e) {
            log.error("Redis 로그 저장 실패: logId={}, error={}", activityLog.getLogId(), e.getMessage(), e);
            throw new RuntimeException("Redis save failed", e);
        }
    }

    public List<String> getLogsForBatch(int batchSize) {
        try {
            List<String> logIds = new ArrayList<>();
            for (int i = 0; i < batchSize; i++) {
                String logId = stringTemplate.opsForList().rightPop(BATCH_QUEUE_KEY);
                if (logId != null)
                    logIds.add(logId);
                else
                    break;
            }
            log.info("Redis 배치 로그 ID 조회: 요청={}, 조회={}, 남은큐={}",
                    batchSize, logIds.size(), getBatchQueueSize());
            return logIds;
        } catch (Exception e) {
            log.error("Redis 배치 로그 ID 조회 실패: batchSize={}, error={}", batchSize, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    public UserActivityLog getLog(String logId) {
        String key = LOG_KEY_PREFIX + logId;
        try {
            log.debug("Redis 로그 조회 시작: logId={}", logId);

            // 1) 정상 경로: 전용 템플릿
            UserActivityLog obj = userLogTemplate.opsForValue().get(key);
            if (obj != null) {
                log.debug("Redis 로그 조회 성공 (전용템플릿): logId={}, userId={}, actionType={}",
                        logId, obj.getUserId(), obj.getActionType());
                return obj;
            }

            // 2) 레거시 백업 경로: 과거 Object 템플릿으로 저장된 값 복구
            Object legacy = legacyObjectTemplate.opsForValue().get(key);
            if (legacy instanceof UserActivityLog) {
                log.debug("Redis 로그 조회 성공 (레거시): logId={}", logId);
                return (UserActivityLog) legacy;
            }
            if (legacy instanceof Map) {
                try {
                    UserActivityLog converted = objectMapper.convertValue(legacy, UserActivityLog.class);
                    log.warn("Redis 레거시 Map 변환 성공: logId={}, userId={}", logId, converted.getUserId());
                    return converted;
                } catch (IllegalArgumentException ce) {
                    log.error("Redis 레거시 Map 변환 실패: logId={}, err={}", logId, ce.getMessage());
                }
            }

            // 3) 최후의 수단: raw bytes 읽기 후 역직렬화 시도
            byte[] raw = getRaw(key);
            if (raw != null && raw.length > 0) {
                try {
                    Map<?, ?> asMap = objectMapper.readValue(raw, Map.class);
                    UserActivityLog converted = objectMapper.convertValue(asMap, UserActivityLog.class);
                    log.warn("Redis raw bytes 복구 성공: logId={}, userId={}", logId, converted.getUserId());
                    return converted;
                } catch (Exception ex) {
                    log.error("Redis raw bytes 복구 실패: logId={}, err={}", logId, ex.getMessage());
                }
            }

            log.warn("Redis 로그 조회 실패 (데이터 없음): logId={}", logId);
            return null;

        } catch (Exception e) {
            log.error("Redis 로그 조회 중 오류: logId={}, error={}", logId, e.getMessage(), e);
            return null;
        }
    }

    private byte[] getRaw(String key) {
        try {
            return userLogTemplate.execute(
                    (RedisCallback<byte[]>) connection -> connection.get(key.getBytes(StandardCharsets.UTF_8)));
        } catch (DataAccessException e) {
            log.debug("Redis raw 조회 실패: {}", e.getMessage());
            return null;
        }
    }

    public void deleteLog(String logId) {
        try {
            String key = LOG_KEY_PREFIX + logId;
            Boolean deleted = userLogTemplate.delete(key);

            for (String priority : List.of("normal", "high", "critical")) {
                String priorityKey = PRIORITY_QUEUE_PREFIX + priority;
                stringTemplate.opsForZSet().remove(priorityKey, logId);
            }

            log.debug("Redis 로그 삭제 완료: logId={}, deleted={}", logId, deleted);
        } catch (Exception e) {
            log.error("Redis 로그 삭제 실패: logId={}, error={}", logId, e.getMessage());
        }
    }

    public long getBatchQueueSize() {
        try {
            Long size = stringTemplate.opsForList().size(BATCH_QUEUE_KEY);
            return size != null ? size : 0L;
        } catch (Exception e) {
            log.error("Redis 큐 크기 조회 실패: {}", e.getMessage());
            return 0L;
        }
    }

    public Set<String> getCriticalLogs(int limit) {
        try {
            String criticalKey = PRIORITY_QUEUE_PREFIX + "critical";
            Set<String> res = Optional.ofNullable(
                    stringTemplate.opsForZSet().range(criticalKey, 0, limit - 1))
                    .orElseGet(Collections::emptySet);
            log.debug("Redis critical 로그 조회: count={}", res.size());
            return res;
        } catch (Exception e) {
            log.error("Redis critical 로그 조회 실패: {}", e.getMessage());
            return Set.of();
        }
    }

    public String getQueueStatus() {
        try {
            long batchQueueSize = getBatchQueueSize();
            Long normalSize = stringTemplate.opsForZSet().count(PRIORITY_QUEUE_PREFIX + "normal",
                    Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
            Long highSize = stringTemplate.opsForZSet().count(PRIORITY_QUEUE_PREFIX + "high",
                    Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
            Long criticalSize = stringTemplate.opsForZSet().count(PRIORITY_QUEUE_PREFIX + "critical",
                    Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);

            String status = String.format("batch=%d, normal=%d, high=%d, critical=%d",
                    batchQueueSize,
                    normalSize != null ? normalSize : 0L,
                    highSize != null ? highSize : 0L,
                    criticalSize != null ? criticalSize : 0L);
            log.debug("Redis 큐 상태: {}", status);
            return status;
        } catch (Exception e) {
            log.error("Redis 큐 상태 조회 실패: {}", e.getMessage());
            return "failed: " + e.getMessage();
        }
    }

    public List<String> peekBatchQueue(int count) {
        try {
            List<String> results = stringTemplate.opsForList().range(BATCH_QUEUE_KEY, -count, -1);
            return results != null ? results : new ArrayList<>();
        } catch (Exception e) {
            log.error("Redis 큐 peek 실패: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    public boolean isRedisConnected() {
        try {
            stringTemplate.hasKey("connection_test");
            return true;
        } catch (Exception e) {
            log.error("Redis 연결 테스트 실패: {}", e.getMessage());
            return false;
        }
    }

    public Map<String, Object> getDebugStatistics() {
        Map<String, Object> stats = new HashMap<>();
        try {
            stats.put("batchQueueSize", getBatchQueueSize());
            stats.put("queueStatus", getQueueStatus());
            stats.put("redisConnected", isRedisConnected());
            stats.put("sampleLogIds", peekBatchQueue(5));
            stats.put("timestamp", System.currentTimeMillis());

            log.debug("Redis 디버그 통계: {}", stats);
            return stats;
        } catch (Exception e) {
            stats.put("error", e.getMessage());
            log.error("Redis 디버그 통계 수집 실패: {}", e.getMessage());
            return stats;
        }
    }
}