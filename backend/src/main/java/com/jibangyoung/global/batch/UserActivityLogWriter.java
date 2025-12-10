package com.jibangyoung.global.batch;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.jibangyoung.global.common.UserActivityLog;
import com.jibangyoung.global.repository.UserActivityLogRedisRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 완전 수정된 DB 저장 Writer (수정: userId null 허용)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserActivityLogWriter implements ItemWriter<UserActivityLog> {

    private final JdbcTemplate jdbcTemplate;
    private final UserActivityLogRedisRepository redisRepository;

    private static final String INSERT_SQL = """
            INSERT INTO user_activity_event (
                user_id, region_id, action_type, ref_id, parent_ref_id,
                action_value, score_delta, meta, ip_addr, user_agent,
                platform, lang, status, memo, created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    @Override
    @Transactional
    public void write(Chunk<? extends UserActivityLog> chunk) throws Exception {
        List<? extends UserActivityLog> items = chunk.getItems();

        if (items.isEmpty()) {
            log.debug("💾 저장할 로그가 없습니다.");
            return;
        }

        log.info("💾 DB 저장 시작: count={}", items.size());

        // 저장할 데이터 상세 로그
        for (int i = 0; i < Math.min(3, items.size()); i++) {
            UserActivityLog sample = items.get(i);
            log.info("💾 샘플 데이터 [{}]: logId={}, actionType={}, userId={}, regionId={}, refId={}",
                    i, sample.getLogId(), sample.getActionType(), sample.getUserId(),
                    sample.getRegionId(), sample.getRefId());
        }

        try {
            // DB 연결 테스트
            int connectionTest = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            log.info("💾 DB 연결 확인: {}", connectionTest);

            // 데이터 전처리 및 검증 (수정: userId null 허용)
            List<UserActivityLog> validItems = preprocessItems(items);

            if (validItems.isEmpty()) {
                log.warn("💾 유효한 데이터가 없어서 저장을 스킵합니다.");
                return;
            }

            // 배치 INSERT 실행
            long startTime = System.currentTimeMillis();
            int[] results = jdbcTemplate.batchUpdate(INSERT_SQL, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    UserActivityLog activityLog = validItems.get(i);
                    setParametersSafely(ps, activityLog, i);
                }

                @Override
                public int getBatchSize() {
                    return validItems.size();
                }
            });

            long executionTime = System.currentTimeMillis() - startTime;
            log.info("💾 배치 INSERT 완료: 실행시간={}ms, 결과배열크기={}", executionTime, results.length);

            // 결과 분석
            List<String> successLogIds = new ArrayList<>();
            List<String> failedLogIds = new ArrayList<>();

            for (int i = 0; i < results.length; i++) {
                UserActivityLog currentLog = validItems.get(i);
                int result = results[i];

                if (result > 0) {
                    successLogIds.add(currentLog.getLogId());
                } else {
                    failedLogIds.add(currentLog.getLogId());
                    log.warn("💾 저장 실패: logId={}, result={}", currentLog.getLogId(), result);
                }
            }

            log.info("💾 DB 저장 결과: total={}, success={}, failed={}",
                    validItems.size(), successLogIds.size(), failedLogIds.size());

            // 실제 DB 저장 확인
            verifyDatabaseSave(validItems.size());

            // Redis에서 성공한 로그만 삭제
            if (!successLogIds.isEmpty()) {
                deleteSuccessLogsFromRedis(successLogIds);
            }

            // 실패한 로그 상세 정보
            if (!failedLogIds.isEmpty()) {
                log.error("💾 실패한 로그들: {}", failedLogIds);
            }

        } catch (Exception e) {
            log.error("💾 배치 저장 중 치명적 오류: count={}", items.size(), e);

            // 실패한 로그 상세 정보
            for (int i = 0; i < items.size(); i++) {
                UserActivityLog item = items.get(i);
                log.error("💾 실패 상세 [{}]: logId={}, actionType={}, userId={}",
                        i, item.getLogId(), item.getActionType(), item.getUserId());
            }
            throw e;
        }
    }

    /**
     * 데이터 전처리 및 검증 (수정: userId null 허용)
     */
    private List<UserActivityLog> preprocessItems(List<? extends UserActivityLog> items) {
        List<UserActivityLog> validItems = new ArrayList<>();

        for (UserActivityLog item : items) {
            try {
                // 필수 필드 검증 (userId는 null 허용)
                if (item.getLogId() == null) {
                    log.warn("💾 logId가 null인 항목 스킵: {}", item);
                    continue;
                }

                if (item.getActionType() == null || item.getActionType().trim().isEmpty()) {
                    log.warn("💾 actionType이 null/empty인 항목 스킵: logId={}", item.getLogId());
                    continue;
                }

                if (item.getCreatedAt() == null) {
                    log.warn("💾 createdAt이 null인 항목 스킵: logId={}", item.getLogId());
                    continue;
                }

                // userId는 null 허용 - DB 저장 시 0으로 변환됨
                if (item.getUserId() == null) {
                    log.debug("💾 userId가 null인 항목 (익명 사용자): logId={}", item.getLogId());
                }

                // regionId 타입 안전성 검증
                if (item.getRegionId() != null) {
                    long regionIdValue = item.getRegionId();
                    if (regionIdValue > Integer.MAX_VALUE || regionIdValue < Integer.MIN_VALUE) {
                        log.warn("💾 regionId 값이 Integer 범위를 초과: logId={}, regionId={}",
                                item.getLogId(), regionIdValue);
                        continue;
                    }
                }

                validItems.add(item);

            } catch (Exception e) {
                log.error("💾 데이터 전처리 중 오류: logId={}", item.getLogId(), e);
            }
        }

        log.info("💾 데이터 전처리 완료: 원본={}, 유효={}", items.size(), validItems.size());
        return validItems;
    }

    /**
     * PreparedStatement 파라미터 안전하게 설정 (수정: userId null 허용)
     */
    private void setParametersSafely(PreparedStatement ps, UserActivityLog activityLog, int index) throws SQLException {
        try {
            // 1. user_id (NOT NULL) - null이면 0으로 설정
            Long userId = activityLog.getUserIdForDb(); // null이면 0L 반환
            ps.setLong(1, userId);

            // 2. region_id (NOT NULL) - Long을 Integer로 안전하게 변환
            Integer regionId = null;
            if (activityLog.getRegionId() != null) {
                long regionIdLong = activityLog.getRegionId();
                if (regionIdLong <= Integer.MAX_VALUE && regionIdLong >= Integer.MIN_VALUE) {
                    regionId = (int) regionIdLong;
                } else {
                    log.warn("💾 regionId 값이 너무 큼, 기본값 사용: logId={}, regionId={}",
                            activityLog.getLogId(), regionIdLong);
                    regionId = 0; // 기본값
                }
            } else {
                regionId = 0; // 기본값
            }
            ps.setInt(2, regionId);

            // 3. action_type (NOT NULL)
            String actionType = activityLog.getActionType();
            if (actionType == null || actionType.trim().isEmpty()) {
                actionType = "UNKNOWN";
            }
            ps.setString(3, actionType);

            // 4. ref_id (nullable)
            if (activityLog.getRefId() != null) {
                ps.setLong(4, activityLog.getRefId());
            } else {
                ps.setNull(4, java.sql.Types.BIGINT);
            }

            // 5. parent_ref_id (nullable)
            if (activityLog.getParentRefId() != null) {
                ps.setLong(5, activityLog.getParentRefId());
            } else {
                ps.setNull(5, java.sql.Types.BIGINT);
            }

            // 6. action_value (nullable) - Object를 안전하게 Integer로 변환
            Integer actionValue = null;
            if (activityLog.getActionValue() != null) {
                try {
                    if (activityLog.getActionValue() instanceof Number) {
                        actionValue = ((Number) activityLog.getActionValue()).intValue();
                    } else {
                        String strValue = activityLog.getActionValue().toString();
                        if (!strValue.trim().isEmpty()) {
                            actionValue = Integer.parseInt(strValue);
                        }
                    }
                } catch (NumberFormatException e) {
                    log.warn("💾 action_value 변환 실패: logId={}, value={}",
                            activityLog.getLogId(), activityLog.getActionValue());
                }
            }

            if (actionValue != null) {
                ps.setInt(6, actionValue);
            } else {
                ps.setNull(6, java.sql.Types.INTEGER);
            }

            // 7. score_delta (기본값 0)
            int scoreDelta = activityLog.getScoreDelta() != null ? activityLog.getScoreDelta() : 0;
            ps.setInt(7, scoreDelta);

            // 8. meta (nullable, JSON)
            String meta = activityLog.getMeta();
            if (meta != null && meta.length() > 4000) { // JSON 필드 크기 제한
                meta = meta.substring(0, 4000);
                log.warn("💾 meta 필드 잘림: logId={}", activityLog.getLogId());
            }
            ps.setString(8, meta);

            // 9. ip_addr (nullable)
            ps.setString(9, activityLog.getIpAddr());

            // 10. user_agent (nullable, 길이 제한)
            String userAgent = activityLog.getUserAgent();
            if (userAgent != null && userAgent.length() > 1000) {
                userAgent = userAgent.substring(0, 1000);
            }
            ps.setString(10, userAgent);

            // 11. platform (nullable)
            ps.setString(11, activityLog.getPlatform());

            // 12. lang (nullable)
            ps.setString(12, activityLog.getLang());

            // 13. status (기본값 'ACTIVE')
            String status = activityLog.getStatus();
            if (status == null || status.trim().isEmpty()) {
                status = "ACTIVE";
            }
            ps.setString(13, status);

            // 14. memo (nullable)
            ps.setString(14, activityLog.getMemo());

            // 15, 16. created_at, updated_at (NOT NULL)
            Timestamp now = new Timestamp(System.currentTimeMillis());
            Timestamp createdAt = activityLog.getCreatedAt() != null ? Timestamp.valueOf(activityLog.getCreatedAt())
                    : now;
            Timestamp updatedAt = activityLog.getUpdatedAt() != null ? Timestamp.valueOf(activityLog.getUpdatedAt())
                    : now;

            ps.setTimestamp(15, createdAt);
            ps.setTimestamp(16, updatedAt);

            log.debug("💾 PreparedStatement 설정 완료 [{}]: logId={}, userId={}, actionType={}",
                    index, activityLog.getLogId(), userId, actionType);

        } catch (SQLException e) {
            log.error("💾 PreparedStatement 설정 실패 [{}]: logId={}, error={}",
                    index, activityLog.getLogId(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 실제 DB 저장 확인
     */
    private void verifyDatabaseSave(int expectedCount) {
        try {
            String verifySql = "SELECT COUNT(*) FROM user_activity_event WHERE created_at >= DATE_SUB(NOW(), INTERVAL 1 MINUTE)";
            int recentCount = jdbcTemplate.queryForObject(verifySql, Integer.class);
            log.info("💾 DB 검증: 최근 1분간 저장된 로그 개수={}, 방금 저장 시도={}", recentCount, expectedCount);

            // 최근 로그 상세 확인
            String detailSql = """
                    SELECT id, action_type, user_id, region_id, status, created_at
                    FROM user_activity_event
                    WHERE created_at >= DATE_SUB(NOW(), INTERVAL 30 SECOND)
                    ORDER BY created_at DESC
                    LIMIT 3
                    """;

            List<Map<String, Object>> recentLogs = jdbcTemplate.queryForList(detailSql);
            log.info("💾 최근 저장된 로그 샘플: {}", recentLogs);

        } catch (Exception e) {
            log.error("💾 DB 검증 중 오류", e);
        }
    }

    /**
     * Redis에서 성공한 로그 삭제
     */
    private void deleteSuccessLogsFromRedis(List<String> successLogIds) {
        if (successLogIds.isEmpty()) {
            return;
        }

        try {
            int deletedCount = 0;
            for (String logId : successLogIds) {
                redisRepository.deleteLog(logId);
                deletedCount++;
            }
            log.info("💾 Redis 로그 삭제 완료: count={}", deletedCount);
        } catch (Exception e) {
            log.error("💾 Redis 로그 삭제 실패: count={}", successLogIds.size(), e);
        }
    }
}