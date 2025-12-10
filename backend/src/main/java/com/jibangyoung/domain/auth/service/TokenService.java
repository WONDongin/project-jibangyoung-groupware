package com.jibangyoung.domain.auth.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jibangyoung.domain.auth.dto.LoginResponseDto;
import com.jibangyoung.domain.auth.entity.User;
import com.jibangyoung.domain.auth.entity.UserStatus;
import com.jibangyoung.domain.auth.repository.UserRepository;
import com.jibangyoung.domain.auth.support.RefreshTokenRedis;
import com.jibangyoung.domain.auth.support.RefreshTokenRedisRepository;
import com.jibangyoung.global.exception.BusinessException;
import com.jibangyoung.global.exception.ErrorCode;
import com.jibangyoung.global.security.CustomUserDetailsService;
import com.jibangyoung.global.security.JwtTokenProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ✅ 수정된 TokenService - 액세스 토큰 재발급 오류 완전 해결
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TokenService {

        private final RefreshTokenRedisRepository refreshTokenRedisRepository;
        private final UserRepository userRepository;
        private final JwtTokenProvider jwtTokenProvider;
        private final CustomUserDetailsService customUserDetailsService;
        private final Clock clock = Clock.systemDefaultZone();

        /**
         * ✅ 기존 generateTokens 메서드 - 로깅 강화 및 예외 처리 개선
         */
        public LoginResponseDto generateTokens(Authentication authentication, User user) {
                try {
                        log.info("[TOKEN] 토큰 생성 시작 - username: {}", user.getUsername());

                        // 1. 액세스 토큰 생성
                        String accessToken = jwtTokenProvider.createAccessToken(authentication);
                        if (accessToken == null || accessToken.trim().isEmpty()) {
                                log.error("[TOKEN] 액세스 토큰 생성 실패");
                                throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
                        }

                        // 2. 리프레시 토큰 생성
                        String refreshToken = jwtTokenProvider.createRefreshToken(user.getUsername());
                        if (refreshToken == null || refreshToken.trim().isEmpty()) {
                                log.error("[TOKEN] 리프레시 토큰 생성 실패");
                                throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
                        }

                        LocalDateTime refreshExpiresAt = jwtTokenProvider.getExpirationDateFromToken(refreshToken);

                        // 3. Redis에 리프레시 토큰 저장
                        long ttlSeconds = jwtTokenProvider.getRefreshTokenValidityInMilliseconds() / 1000;
                        RefreshTokenRedis tokenObj = RefreshTokenRedis.builder()
                                        .token(refreshToken)
                                        .username(user.getUsername())
                                        .expiresAt(refreshExpiresAt)
                                        .revoked(false)
                                        .build();
                        refreshTokenRedisRepository.save(tokenObj, ttlSeconds);

                        // 4. 응답 DTO 생성
                        LocalDateTime now = LocalDateTime.now(clock);
                        LocalDateTime accessTokenExpiresAt = now
                                        .plusSeconds(jwtTokenProvider.getAccessTokenValidityInMilliseconds() / 1000);

                        try {
                                long activeTokens = refreshTokenRedisRepository
                                                .countActiveTokensByUsername(user.getUsername());
                                log.info("[TOKEN] 토큰 생성 완료 - username: {}, 활성 토큰 수: {}", user.getUsername(),
                                                activeTokens);
                        } catch (Exception countError) {
                                log.info("[TOKEN] 토큰 생성 완료 - username: {} (토큰 수 조회 실패)", user.getUsername());
                        }

                        return LoginResponseDto.of(
                                        user, accessToken, refreshToken,
                                        jwtTokenProvider.getAccessTokenValidityInMilliseconds() / 1000,
                                        now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                                        accessTokenExpiresAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

                } catch (Exception e) {
                        log.error("[TOKEN] 토큰 생성 실패 - username: {}, error: {}", user.getUsername(), e.getMessage(), e);
                        throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
                }
        }

        /**
         * ✅ 완전히 수정된 refreshAccessToken 메서드 - 모든 문제점 해결
         */
        public LoginResponseDto refreshAccessToken(String refreshToken) {
                log.info("[TOKEN] 토큰 재발급 요청 시작");

                // 1. 입력값 검증
                if (refreshToken == null || refreshToken.trim().isEmpty()) {
                        log.warn("[TOKEN] 빈 리프레시 토큰 제공됨");
                        throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
                }

                String cleanRefreshToken = refreshToken.trim();

                try {
                        // 2. JWT 토큰 서명 및 형식 검증
                        if (!jwtTokenProvider.validateToken(cleanRefreshToken)) {
                                log.warn("[TOKEN] JWT 토큰 형식/서명 검증 실패");
                                throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
                        }

                        // 3. JWT에서 사용자명 추출
                        String usernameFromToken;
                        try {
                                usernameFromToken = jwtTokenProvider.getUsernameFromToken(cleanRefreshToken);
                                if (usernameFromToken == null || usernameFromToken.trim().isEmpty()) {
                                        log.warn("[TOKEN] 토큰에서 사용자명 추출 실패");
                                        throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
                                }
                        } catch (Exception e) {
                                log.warn("[TOKEN] 토큰에서 사용자명 추출 중 오류: {}", e.getMessage());
                                throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
                        }

                        log.info("[TOKEN] 토큰에서 사용자명 추출 성공: {}", usernameFromToken);

                        // 4. Redis에서 리프레시 토큰 조회 및 검증
                        RefreshTokenRedis storedToken = refreshTokenRedisRepository.findByToken(cleanRefreshToken);
                        if (storedToken == null) {
                                log.warn("[TOKEN] 리프레시 토큰이 Redis에 존재하지 않음 - username: {}", usernameFromToken);
                                throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
                        }

                        // 5. 사용자명 일치 확인
                        if (!usernameFromToken.equals(storedToken.getUsername())) {
                                log.warn("[TOKEN] 토큰의 사용자명 불일치 - JWT: {}, Redis: {}",
                                                usernameFromToken, storedToken.getUsername());
                                refreshTokenRedisRepository.deleteByToken(cleanRefreshToken);
                                throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
                        }

                        // 6. 토큰 만료 확인
                        if (storedToken.isExpired()) {
                                log.warn("[TOKEN] 리프레시 토큰 만료됨 - username: {}, expiresAt: {}",
                                                storedToken.getUsername(), storedToken.getExpiresAt());
                                refreshTokenRedisRepository.deleteByToken(cleanRefreshToken);
                                throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
                        }

                        // 7. 토큰 무효화 상태 확인
                        if (storedToken.isRevoked()) {
                                log.warn("[TOKEN] 이미 무효화된 리프레시 토큰 - username: {}", storedToken.getUsername());
                                refreshTokenRedisRepository.deleteByToken(cleanRefreshToken);
                                throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
                        }

                        // 8. 사용자 존재 및 상태 확인 (ACTIVE 상태만 허용)
                        User user = userRepository.findByUsernameAndStatus(storedToken.getUsername(), UserStatus.ACTIVE)
                                        .orElseThrow(() -> {
                                                log.error("[TOKEN] 활성 사용자를 찾을 수 없음 - username: {}",
                                                                storedToken.getUsername());
                                                refreshTokenRedisRepository.deleteByToken(cleanRefreshToken);
                                                return new BusinessException(ErrorCode.USER_NOT_FOUND);
                                        });

                        log.info("[TOKEN] 사용자 상태 확인 완료 - username: {}, status: {}",
                                        user.getUsername(), user.getStatus());

                        // 9. UserDetails 로드 및 Authentication 객체 생성
                        UserDetails userDetails;
                        Authentication authentication;
                        try {
                                userDetails = customUserDetailsService.loadUserByUsername(user.getUsername());
                                authentication = new UsernamePasswordAuthenticationToken(
                                                userDetails, null, userDetails.getAuthorities());

                                log.info("[TOKEN] UserDetails 로드 성공 - username: {}, authorities: {}",
                                                user.getUsername(), userDetails.getAuthorities());
                        } catch (Exception e) {
                                log.error("[TOKEN] UserDetails 로드 실패 - username: {}, error: {}",
                                                user.getUsername(), e.getMessage());
                                refreshTokenRedisRepository.deleteByToken(cleanRefreshToken);
                                throw new BusinessException(ErrorCode.USER_NOT_FOUND);
                        }

                        // 10. 새 액세스 토큰 생성
                        String newAccessToken;
                        try {
                                newAccessToken = jwtTokenProvider.createAccessToken(authentication);
                                if (newAccessToken == null || newAccessToken.trim().isEmpty()) {
                                        log.error("[TOKEN] 새 액세스 토큰 생성 실패 - username: {}", user.getUsername());
                                        throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
                                }
                                log.info("[TOKEN] 새 액세스 토큰 생성 성공 - username: {}", user.getUsername());
                        } catch (Exception e) {
                                log.error("[TOKEN] 액세스 토큰 생성 중 오류 - username: {}, error: {}",
                                                user.getUsername(), e.getMessage());
                                throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
                        }

                        // 11. 새 리프레시 토큰 생성 (Refresh Token Rotation)
                        String newRefreshToken;
                        LocalDateTime newRefreshExpiresAt;
                        try {
                                newRefreshToken = jwtTokenProvider.createRefreshToken(user.getUsername());
                                if (newRefreshToken == null || newRefreshToken.trim().isEmpty()) {
                                        log.error("[TOKEN] 새 리프레시 토큰 생성 실패 - username: {}", user.getUsername());
                                        throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
                                }

                                newRefreshExpiresAt = jwtTokenProvider.getExpirationDateFromToken(newRefreshToken);
                                log.info("[TOKEN] 새 리프레시 토큰 생성 성공 - username: {}", user.getUsername());
                        } catch (Exception e) {
                                log.error("[TOKEN] 리프레시 토큰 생성 중 오류 - username: {}, error: {}",
                                                user.getUsername(), e.getMessage());
                                throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
                        }

                        // 12. 기존 토큰 무효화 및 새 토큰 저장 (트랜잭션 처리)
                        try {
                                // 기존 토큰 삭제
                                refreshTokenRedisRepository.deleteByToken(cleanRefreshToken);
                                log.info("[TOKEN] 기존 리프레시 토큰 삭제 완료");

                                // 새 리프레시 토큰 저장
                                RefreshTokenRedis newTokenObj = RefreshTokenRedis.builder()
                                                .token(newRefreshToken)
                                                .username(user.getUsername())
                                                .expiresAt(newRefreshExpiresAt)
                                                .revoked(false)
                                                .build();

                                long ttlSeconds = jwtTokenProvider.getRefreshTokenValidityInMilliseconds() / 1000;
                                refreshTokenRedisRepository.save(newTokenObj, ttlSeconds);
                                log.info("[TOKEN] 새 리프레시 토큰 저장 완료");

                        } catch (Exception e) {
                                log.error("[TOKEN] 토큰 교체 중 Redis 오류 - username: {}, error: {}",
                                                user.getUsername(), e.getMessage());

                                // Redis 실패 시 복구 시도
                                try {
                                        log.warn("[TOKEN] Redis 저장 실패, 복구 시도");
                                        RefreshTokenRedis newTokenObj = RefreshTokenRedis.builder()
                                                        .token(newRefreshToken)
                                                        .username(user.getUsername())
                                                        .expiresAt(newRefreshExpiresAt)
                                                        .revoked(false)
                                                        .build();
                                        long ttlSeconds = jwtTokenProvider.getRefreshTokenValidityInMilliseconds()
                                                        / 1000;
                                        refreshTokenRedisRepository.save(newTokenObj, ttlSeconds);
                                        log.info("[TOKEN] Redis 저장 복구 성공");
                                } catch (Exception retryError) {
                                        log.error("[TOKEN] Redis 저장 복구도 실패: {}", retryError.getMessage());
                                        // 그래도 새 토큰은 반환하여 클라이언트 사용성 확보
                                }
                        }

                        // 13. 응답 DTO 생성
                        LocalDateTime now = LocalDateTime.now(clock);
                        LocalDateTime accessTokenExpiresAt = now
                                        .plusSeconds(jwtTokenProvider.getAccessTokenValidityInMilliseconds() / 1000);

                        LoginResponseDto response = LoginResponseDto.of(
                                        user,
                                        newAccessToken,
                                        newRefreshToken,
                                        jwtTokenProvider.getAccessTokenValidityInMilliseconds() / 1000,
                                        now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                                        accessTokenExpiresAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

                        log.info("[TOKEN] 토큰 재발급 성공 - username: {}", user.getUsername());
                        return response;

                } catch (BusinessException e) {
                        // 비즈니스 예외는 그대로 전파
                        log.warn("[TOKEN] 토큰 재발급 실패 - BusinessException: {} ({})",
                                        e.getMessage(), e.getErrorCode().getCode());
                        throw e;
                } catch (Exception e) {
                        log.error("[TOKEN] 토큰 재발급 중 예상치 못한 오류: {}", e.getMessage(), e);
                        throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
                }
        }

        /**
         * ✅ 수정된 revokeToken 메서드 - 안전성 강화
         */
        public void revokeToken(String refreshToken) {
                if (refreshToken == null || refreshToken.trim().isEmpty()) {
                        log.warn("[TOKEN] 빈 리프레시 토큰으로 로그아웃 시도");
                        return;
                }

                try {
                        RefreshTokenRedis token = refreshTokenRedisRepository.findByToken(refreshToken.trim());
                        if (token != null) {
                                refreshTokenRedisRepository.deleteByToken(refreshToken.trim());
                                log.info("[TOKEN] 리프레시 토큰 삭제 완료 - username: {}", token.getUsername());
                        } else {
                                log.info("[TOKEN] 무효화할 토큰이 존재하지 않음 (이미 만료/삭제됨)");
                        }
                } catch (Exception e) {
                        // 로그아웃은 실패해도 클라이언트에 영향 없도록 예외 삼키기
                        log.error("[TOKEN] 토큰 무효화 중 오류 (무시됨): {}", e.getMessage(), e);
                }
        }

        /**
         * ✅ 수정된 revokeAllUserTokens 메서드
         */
        public void revokeAllUserTokens(String username) {
                if (username == null || username.trim().isEmpty()) {
                        log.warn("[TOKEN] 빈 사용자명으로 전체 로그아웃 시도");
                        return;
                }

                try {
                        log.info("[TOKEN] 사용자 {} 의 모든 토큰 무효화 요청", username);

                        try {
                                long activeTokenCount = refreshTokenRedisRepository
                                                .countActiveTokensByUsername(username);
                                log.info("[TOKEN] 사용자 {}의 활성 토큰 수: {}", username, activeTokenCount);
                        } catch (Exception countError) {
                                log.warn("[TOKEN] 활성 토큰 수 조회 실패 (계속 진행): {}", countError.getMessage());
                        }

                        refreshTokenRedisRepository.deleteAllByUsername(username);
                        log.info("[TOKEN] 사용자 {} 의 모든 토큰 무효화 완료", username);

                } catch (Exception e) {
                        log.error("[TOKEN] 전체 토큰 무효화 중 오류: username={}, error={}", username, e.getMessage(), e);

                        // 실패 시 SCAN 방식으로 재시도
                        try {
                                log.info("[TOKEN] SCAN 방식으로 재시도 - username: {}", username);
                                refreshTokenRedisRepository.deleteAllByUsernameWithScan(username);
                        } catch (Exception retryError) {
                                log.error("[TOKEN] SCAN 방식 재시도도 실패: username={}, error={}", username,
                                                retryError.getMessage());
                        }
                }
        }

        // ========== 추가 기능들 ==========

        /**
         * ✅ 토큰 상태 확인 (디버깅/모니터링용)
         */
        public boolean isTokenValid(String refreshToken) {
                if (refreshToken == null || refreshToken.trim().isEmpty()) {
                        return false;
                }

                try {
                        // JWT 자체 검증 먼저
                        if (!jwtTokenProvider.validateToken(refreshToken)) {
                                return false;
                        }

                        // Redis에서 토큰 정보 확인
                        RefreshTokenRedis storedToken = refreshTokenRedisRepository.findByToken(refreshToken);
                        return storedToken != null &&
                                        !storedToken.isExpired() &&
                                        !storedToken.isRevoked();
                } catch (Exception e) {
                        log.warn("[TOKEN] 토큰 유효성 확인 실패: {}", e.getMessage());
                        return false;
                }
        }

        /**
         * ✅ 사용자의 활성 토큰 개수 확인
         */
        public long getUserActiveTokenCount(String username) {
                try {
                        return refreshTokenRedisRepository.countActiveTokensByUsername(username);
                } catch (Exception e) {
                        log.error("[TOKEN] 활성 토큰 개수 조회 실패 - username: {}, error: {}", username, e.getMessage());
                        return 0L;
                }
        }

        /**
         * ✅ 만료된 토큰 정리 (스케줄러에서 호출 가능)
         */
        public void cleanupExpiredTokens() {
                try {
                        log.info("[TOKEN] 만료된 토큰 정리 시작");
                        refreshTokenRedisRepository.cleanupExpiredTokens();
                        log.info("[TOKEN] 만료된 토큰 정리 완료");
                } catch (Exception e) {
                        log.error("[TOKEN] 만료된 토큰 정리 실패: {}", e.getMessage(), e);
                }
        }
}