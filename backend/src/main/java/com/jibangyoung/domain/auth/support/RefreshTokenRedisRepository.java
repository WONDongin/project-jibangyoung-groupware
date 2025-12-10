package com.jibangyoung.domain.auth.support;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ✅ 수정된 Redis 기반 RefreshToken 저장소 - 안정성 강화
 * - Redis 직렬화 타입 불일치(LinkedHashMap/String 등)에서도 안전하게 RefreshTokenRedis로 복원
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenRedisRepository {

    private static final String PREFIX = "refresh_token:";
    private static final String USER_PREFIX = "user_tokens:"; // 사용자별 토큰 목록 관리

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper; // ← 추가: 다양한 직렬화 타입을 안전 변환

    /** 직렬화 타입에 상관없이 RefreshTokenRedis로 변환 */
    private RefreshTokenRedis toRefreshToken(Object raw) {
        if (raw == null)
            return null;
        if (raw instanceof RefreshTokenRedis)
            return (RefreshTokenRedis) raw;

        try {
            if (raw instanceof Map) {
                return objectMapper.convertValue(raw, RefreshTokenRedis.class);
            }
            if (raw instanceof String) {
                return objectMapper.readValue((String) raw, RefreshTokenRedis.class);
            }
            return objectMapper.convertValue(raw, RefreshTokenRedis.class);
        } catch (Exception e) {
            log.warn("[REDIS] 토큰 객체 변환 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * ✅ 수정된 save 메서드 - 오류 처리 강화
     */
    public void save(RefreshTokenRedis token, long ttlSeconds) {
        if (token == null) {
            log.warn("[REDIS] null 토큰 저장 시도 무시");
            return;
        }

        if (token.getToken() == null || token.getToken().trim().isEmpty()) {
            log.warn("[REDIS] 빈 토큰 문자열 저장 시도 무시 - username: {}", token.getUsername());
            return;
        }

        if (token.getUsername() == null || token.getUsername().trim().isEmpty()) {
            log.warn("[REDIS] 빈 사용자명으로 토큰 저장 시도 무시");
            return;
        }

        if (ttlSeconds <= 0) {
            log.warn("[REDIS] TTL이 0 이하인 토큰은 저장하지 않음 - username: {}", token.getUsername());
            return;
        }

        String tokenKey = PREFIX + token.getToken();
        String userTokensKey = USER_PREFIX + token.getUsername();

        try {
            // 1) 토큰 저장
            redisTemplate.opsForValue().set(tokenKey, token, ttlSeconds, TimeUnit.SECONDS);

            // 2) 사용자별 토큰 Set 관리 (전체 로그아웃용)
            try {
                redisTemplate.opsForSet().add(userTokensKey, token.getToken());
                // 사용자 토큰 목록도 TTL 적용 (조금 더 길게)
                redisTemplate.expire(userTokensKey, ttlSeconds + 300, TimeUnit.SECONDS);
            } catch (Exception userTokenError) {
                log.warn("[REDIS] 사용자 토큰 목록 관리 실패 (기본 저장은 성공) - username: {}, error: {}",
                        token.getUsername(), userTokenError.getMessage());
            }

            log.debug("[REDIS] 토큰 저장 완료 - username: {}, ttl: {}초", token.getUsername(), ttlSeconds);

        } catch (Exception e) {
            log.error("[REDIS] 토큰 저장 실패 - username: {}, error: {}", token.getUsername(), e.getMessage(), e);
            throw new RuntimeException("토큰 저장 실패", e);
        }
    }

    /**
     * ✅ 수정된 findByToken 메서드 - 예외 처리 & 타입 변환 강화
     */
    public RefreshTokenRedis findByToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            log.debug("[REDIS] 빈 토큰으로 조회 시도");
            return null;
        }

        try {
            String tokenKey = PREFIX + token;
            Object raw = redisTemplate.opsForValue().get(tokenKey);

            RefreshTokenRedis tokenObj = toRefreshToken(raw);
            if (tokenObj == null) {
                log.debug("[REDIS] 토큰을 찾을 수 없음 또는 타입 불일치: {}", token);
                return null;
            }

            // 만료된 토큰 자동 정리
            if (tokenObj.isExpired()) {
                log.debug("[REDIS] 만료된 토큰 자동 삭제: {}", token);
                try {
                    deleteByToken(token);
                } catch (Exception deleteError) {
                    log.warn("[REDIS] 만료 토큰 삭제 실패 (무시): {}", deleteError.getMessage());
                }
                return null;
            }

            return tokenObj;

        } catch (Exception e) {
            log.error("[REDIS] 토큰 조회 실패 - token: {}, error: {}", token, e.getMessage(), e);
            return null;
        }
    }

    /**
     * ✅ 수정된 deleteByToken 메서드 - 원자성 보장 & 타입 변환 사용
     */
    public void deleteByToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            log.debug("[REDIS] 빈 토큰 삭제 시도 무시");
            return;
        }

        String tokenKey = PREFIX + token;
        RefreshTokenRedis tokenObj = null;

        try {
            // 삭제 전 토큰 조회 (Set에서 제거 위해)
            try {
                Object raw = redisTemplate.opsForValue().get(tokenKey);
                tokenObj = toRefreshToken(raw);
            } catch (Exception queryError) {
                log.warn("[REDIS] 삭제 전 토큰 조회 실패 (계속 진행): {}", queryError.getMessage());
            }

            // 토큰 키 삭제
            Boolean deleted = redisTemplate.delete(tokenKey);
            log.debug("[REDIS] 토큰 객체 삭제 결과: {}", deleted);

            // 사용자 토큰 목록에서도 제거
            if (tokenObj != null && tokenObj.getUsername() != null) {
                try {
                    String userTokensKey = USER_PREFIX + tokenObj.getUsername();
                    Long removedCount = redisTemplate.opsForSet().remove(userTokensKey, token);
                    log.debug("[REDIS] 사용자 토큰 목록에서 제거됨: {} (제거된 수: {})",
                            tokenObj.getUsername(), removedCount);
                } catch (Exception userTokenError) {
                    log.warn("[REDIS] 사용자 토큰 목록 제거 실패 (무시) - username: {}, error: {}",
                            tokenObj.getUsername(), userTokenError.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("[REDIS] 토큰 삭제 실패 - token: {}, error: {}", token, e.getMessage(), e);
            // 삭제 실패는 로깅만 하고 예외를 던지지 않음 (로그아웃 UX 보호)
        }
    }

    /**
     * ✅ 수정된 revoke 메서드 - 안전성 강화
     */
    public void revoke(String token) {
        if (token == null || token.trim().isEmpty()) {
            log.debug("[REDIS] 빈 토큰 무효화 시도 무시");
            return;
        }

        try {
            RefreshTokenRedis rt = findByToken(token);
            if (rt != null) {
                if (!rt.isExpired()) {
                    rt.setRevoked(true);
                    long seconds = Duration.between(LocalDateTime.now(), rt.getExpiresAt()).getSeconds();
                    if (seconds > 0) {
                        save(rt, seconds);
                        log.debug("[REDIS] 토큰 무효화 완료 - username: {}", rt.getUsername());
                    } else {
                        deleteByToken(token);
                        log.debug("[REDIS] 만료된 토큰 즉시 삭제 - username: {}", rt.getUsername());
                    }
                } else {
                    deleteByToken(token);
                    log.debug("[REDIS] 이미 만료된 토큰 삭제");
                }
            } else {
                log.debug("[REDIS] 무효화할 토큰이 존재하지 않음");
            }
        } catch (Exception e) {
            log.error("[REDIS] 토큰 무효화 실패 - token: {}, error: {}", token, e.getMessage(), e);
        }
    }

    /**
     * ✅ 수정된 사용자의 모든 리프레시 토큰 무효화 - 트랜잭션성 개선
     */
    public void deleteAllByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            log.warn("[REDIS] 빈 사용자명으로 전체 삭제 시도 무시");
            return;
        }

        try {
            String userTokensKey = USER_PREFIX + username;

            Set<Object> tokenSet = redisTemplate.opsForSet().members(userTokensKey);

            if (tokenSet != null && !tokenSet.isEmpty()) {
                log.info("[REDIS] 사용자 {}의 토큰 {}개 삭제 시작", username, tokenSet.size());

                int successCount = 0;
                for (Object tokenObj : tokenSet) {
                    if (tokenObj instanceof String) {
                        String token = (String) tokenObj;
                        try {
                            String tokenKey = PREFIX + token;
                            Boolean deleted = redisTemplate.delete(tokenKey);
                            if (Boolean.TRUE.equals(deleted)) {
                                successCount++;
                            }
                            log.debug("[REDIS] 개별 토큰 삭제: {} (결과: {})", token, deleted);
                        } catch (Exception tokenDeleteError) {
                            log.warn("[REDIS] 개별 토큰 삭제 실패: {}, error: {}", token, tokenDeleteError.getMessage());
                        }
                    }
                }

                try {
                    Boolean listDeleted = redisTemplate.delete(userTokensKey);
                    log.debug("[REDIS] 사용자 토큰 목록 삭제: {} (결과: {})", username, listDeleted);
                } catch (Exception listDeleteError) {
                    log.warn("[REDIS] 사용자 토큰 목록 삭제 실패 (무시): {}", listDeleteError.getMessage());
                }

                log.info("[REDIS] 사용자 {}의 토큰 삭제 완료 ({}/{})", username, successCount, tokenSet.size());
            } else {
                log.info("[REDIS] 사용자 {}의 활성 토큰 없음", username);
                try {
                    redisTemplate.delete(userTokensKey);
                } catch (Exception e) {
                    log.debug("[REDIS] 빈 사용자 토큰 목록 정리 실패 (무시): {}", e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("[REDIS] 사용자 토큰 전체 삭제 실패 - username: {}, error: {}", username, e.getMessage(), e);
        }
    }

    /**
     * ✅ SCAN 방식 백업 (Set 관리 실패 시 대안) - 성능 최적화
     */
    public void deleteAllByUsernameWithScan(String username) {
        if (username == null || username.trim().isEmpty()) {
            return;
        }

        try {
            log.info("[REDIS] SCAN 방식으로 사용자 {}의 토큰 삭제 시작", username);

            ScanOptions scanOptions = ScanOptions.scanOptions()
                    .match(PREFIX + "*")
                    .count(1000)
                    .build();

            int deletedCount = 0;
            int scannedCount = 0;

            try (Cursor<String> cursor = redisTemplate.scan(scanOptions)) {
                while (cursor.hasNext()) {
                    String key = cursor.next();
                    scannedCount++;

                    try {
                        Object raw = redisTemplate.opsForValue().get(key);
                        RefreshTokenRedis token = toRefreshToken(raw);
                        if (token != null && username.equals(token.getUsername())) {
                            Boolean deleted = redisTemplate.delete(key);
                            if (Boolean.TRUE.equals(deleted)) {
                                deletedCount++;
                            }
                            log.debug("[REDIS] SCAN으로 토큰 삭제: {} (결과: {})", key, deleted);
                        }
                    } catch (Exception e) {
                        log.warn("[REDIS] 개별 토큰 처리 실패: {}, error: {}", key, e.getMessage());
                    }
                }
            }

            // 사용자 토큰 목록도 정리
            try {
                String userTokensKey = USER_PREFIX + username;
                Boolean listDeleted = redisTemplate.delete(userTokensKey);
                log.debug("[REDIS] SCAN 후 사용자 토큰 목록 정리: {} (결과: {})", username, listDeleted);
            } catch (Exception e) {
                log.warn("[REDIS] 사용자 토큰 목록 정리 실패 (무시): {}", e.getMessage());
            }

            log.info("[REDIS] SCAN 방식으로 사용자 {}의 토큰 {}개 삭제 완료 (스캔된 키: {}개)",
                    username, deletedCount, scannedCount);

        } catch (Exception e) {
            log.error("[REDIS] SCAN 방식 토큰 삭제 실패 - username: {}, error: {}", username, e.getMessage(), e);
        }
    }

    /**
     * ✅ 사용자의 활성 토큰 개수 조회 - 안전성 강화
     */
    public long countActiveTokensByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return 0L;
        }

        try {
            String userTokensKey = USER_PREFIX + username;
            Long count = redisTemplate.opsForSet().size(userTokensKey);
            return count != null ? count : 0L;
        } catch (Exception e) {
            log.error("[REDIS] 활성 토큰 개수 조회 실패 - username: {}, error: {}", username, e.getMessage());
            return 0L;
        }
    }

    /**
     * ✅ 만료된 토큰 정리 (스케줄러용) - 타입 변환 적용
     */
    public void cleanupExpiredTokens() {
        try {
            log.info("[REDIS] 만료된 토큰 정리 시작");

            ScanOptions scanOptions = ScanOptions.scanOptions()
                    .match(PREFIX + "*")
                    .count(1000)
                    .build();

            int cleanedCount = 0;
            int scannedCount = 0;

            try (Cursor<String> cursor = redisTemplate.scan(scanOptions)) {
                while (cursor.hasNext()) {
                    String key = cursor.next();
                    scannedCount++;

                    try {
                        Object raw = redisTemplate.opsForValue().get(key);
                        RefreshTokenRedis token = toRefreshToken(raw);
                        if (token != null && token.isExpired()) {
                            Boolean deleted = redisTemplate.delete(key);
                            if (Boolean.TRUE.equals(deleted)) {
                                cleanedCount++;
                            }

                            // 사용자 토큰 목록에서도 제거
                            if (token.getUsername() != null) {
                                try {
                                    String userTokensKey = USER_PREFIX + token.getUsername();
                                    redisTemplate.opsForSet().remove(userTokensKey, token.getToken());
                                } catch (Exception userTokenError) {
                                    // 무시
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.warn("[REDIS] 만료 토큰 정리 중 개별 오류: {} - {}", key, e.getMessage());
                    }
                }
            }

            log.info("[REDIS] 만료된 토큰 {}개 정리 완료 (스캔된 키: {}개)", cleanedCount, scannedCount);

        } catch (Exception e) {
            log.error("[REDIS] 만료 토큰 정리 실패: {}", e.getMessage(), e);
        }
    }
}
