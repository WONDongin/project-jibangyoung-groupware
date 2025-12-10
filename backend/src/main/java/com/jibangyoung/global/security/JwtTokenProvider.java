package com.jibangyoung.global.security;

import java.security.Key;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret; // 반드시 Base64 인코딩된 문자열!

    @Value("${jwt.access-token-validity-ms}")
    private long accessTokenValidityInMilliseconds;

    @Value("${jwt.refresh-token-validity-ms}")
    private long refreshTokenValidityInMilliseconds;

    private final CustomUserDetailsService userDetailsService;

    private Key getSigningKey(String base64Secret) {
        byte[] keyBytes = Base64.getDecoder().decode(base64Secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String createAccessToken(Authentication authentication) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenValidityInMilliseconds);
        String username = authentication.getName();

        String token = Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(jwtSecret), SignatureAlgorithm.HS512)
                .compact();

        log.debug("[JWT] 액세스 토큰 생성 완료 - username: {}, 만료시간: {}", username, expiryDate);
        return token;
    }

    public String createRefreshToken(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenValidityInMilliseconds);

        String token = Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(jwtSecret), SignatureAlgorithm.HS512)
                .compact();

        log.debug("[JWT] 리프레시 토큰 생성 완료 - username: {}, 만료시간: {}", username, expiryDate);
        return token;
    }

    public String getUsernameFromToken(String token) {
        try {
            Claims claims = parseClaims(token);
            String username = claims.getSubject();
            log.debug("[JWT] 토큰에서 사용자명 추출 성공: {}", username);
            return username;
        } catch (Exception e) {
            log.warn("[JWT] 토큰에서 사용자명 추출 실패: {}", e.getMessage());
            throw e;
        }
    }

    public LocalDateTime getExpirationDateFromToken(String token) {
        try {
            Claims claims = parseClaims(token);
            Date exp = claims.getExpiration();
            LocalDateTime expirationTime = LocalDateTime.ofInstant(exp.toInstant(), ZoneId.systemDefault());
            log.debug("[JWT] 토큰 만료시간 추출 성공: {}", expirationTime);
            return expirationTime;
        } catch (Exception e) {
            log.warn("[JWT] 토큰 만료시간 추출 실패: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * ✅ 토큰 유효성 검증 로직 완전 개선
     */
    public boolean validateToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            log.debug("[JWT] 빈 토큰으로 검증 시도");
            return false;
        }

        try {
            // 1. 토큰 형식 기본 검증
            String[] tokenParts = token.split("\\.");
            if (tokenParts.length != 3) {
                log.warn("[JWT] 토큰 형식 오류 - 파트 수: {}", tokenParts.length);
                return false;
            }

            // 2. Claims 파싱 및 서명 검증
            Claims claims = parseClaims(token);

            // 3. 필수 필드 검증
            if (claims.getSubject() == null || claims.getSubject().trim().isEmpty()) {
                log.warn("[JWT] 토큰에 subject(사용자명)가 없음");
                return false;
            }

            if (claims.getExpiration() == null) {
                log.warn("[JWT] 토큰에 만료시간이 없음");
                return false;
            }

            if (claims.getIssuedAt() == null) {
                log.warn("[JWT] 토큰에 발급시간이 없음");
                return false;
            }

            // 4. 시간 관계 검증
            Date now = new Date();
            Date issuedAt = claims.getIssuedAt();
            Date expiration = claims.getExpiration();

            // 발급시간이 미래인 경우
            if (issuedAt.after(now)) {
                log.warn("[JWT] 토큰 발급시간이 현재시간보다 미래임 - issued: {}, now: {}", issuedAt, now);
                return false;
            }

            // 만료시간이 발급시간보다 이전인 경우
            if (expiration.before(issuedAt)) {
                log.warn("[JWT] 토큰 만료시간이 발급시간보다 이전임 - issued: {}, exp: {}", issuedAt, expiration);
                return false;
            }

            // 5. 만료 여부 최종 확인
            if (expiration.before(now)) {
                log.debug("[JWT] 토큰이 만료됨 - exp: {}, now: {}", expiration, now);
                return false;
            }

            log.debug("[JWT] 토큰 검증 성공 - subject: {}", claims.getSubject());
            return true;

        } catch (ExpiredJwtException e) {
            log.debug("[JWT] 토큰 만료됨: {}", e.getMessage());
            return false;
        } catch (MalformedJwtException e) {
            log.warn("[JWT] 토큰 형식 오류: {}", e.getMessage());
            return false;
        } catch (SignatureException e) {
            log.warn("[JWT] 토큰 서명 검증 실패: {}", e.getMessage());
            return false;
        } catch (UnsupportedJwtException e) {
            log.warn("[JWT] 지원되지 않는 토큰: {}", e.getMessage());
            return false;
        } catch (IllegalArgumentException e) {
            log.warn("[JWT] 토큰 파라미터 오류: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("[JWT] 토큰 검증 중 예상치 못한 오류: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * ✅ Authentication 객체 생성 로직 개선
     */
    public Authentication getAuthentication(String token) {
        try {
            String username = getUsernameFromToken(token);

            if (username == null || username.trim().isEmpty()) {
                log.warn("[JWT] 토큰에서 사용자명을 추출할 수 없음");
                return null;
            }

            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            if (userDetails == null) {
                log.warn("[JWT] 사용자를 찾을 수 없음: {}", username);
                return null;
            }

            if (!userDetails.isEnabled()) {
                log.warn("[JWT] 비활성화된 사용자: {}", username);
                return null;
            }

            if (!userDetails.isAccountNonLocked()) {
                log.warn("[JWT] 잠긴 계정: {}", username);
                return null;
            }

            if (!userDetails.isAccountNonExpired()) {
                log.warn("[JWT] 만료된 계정: {}", username);
                return null;
            }

            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());

            log.debug("[JWT] Authentication 객체 생성 성공 - username: {}, authorities: {}",
                    username, userDetails.getAuthorities());

            return authentication;

        } catch (Exception e) {
            log.error("[JWT] Authentication 객체 생성 실패: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * ✅ Claims 파싱 메서드 개선
     */
    private Claims parseClaims(String token) {
        try {
            return Jwts
                    .parserBuilder()
                    .setSigningKey(getSigningKey(jwtSecret))
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            // 만료된 토큰이어도 Claims 정보는 필요할 수 있음 (재발급 시)
            log.debug("[JWT] 만료된 토큰에서 Claims 추출: {}", e.getMessage());
            return e.getClaims();
        } catch (Exception e) {
            log.warn("[JWT] Claims 파싱 실패: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * ✅ 토큰 만료까지 남은 시간 확인 (밀리초)
     */
    public long getTimeUntilExpiration(String token) {
        try {
            Claims claims = parseClaims(token);
            Date expiration = claims.getExpiration();
            Date now = new Date();

            long timeUntilExpiry = expiration.getTime() - now.getTime();
            log.debug("[JWT] 토큰 만료까지 남은 시간: {}ms", timeUntilExpiry);

            return Math.max(0, timeUntilExpiry);
        } catch (Exception e) {
            log.warn("[JWT] 토큰 만료 시간 계산 실패: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * ✅ 토큰이 곧 만료되는지 확인 (5분 이내)
     */
    public boolean isTokenExpiringSoon(String token) {
        try {
            long timeUntilExpiry = getTimeUntilExpiration(token);
            boolean expiringSoon = timeUntilExpiry < (5 * 60 * 1000); // 5분

            if (expiringSoon) {
                log.debug("[JWT] 토큰이 곧 만료됨 - 남은 시간: {}ms", timeUntilExpiry);
            }

            return expiringSoon;
        } catch (Exception e) {
            log.warn("[JWT] 토큰 만료 임박 확인 실패: {}", e.getMessage());
            return true; // 확실하지 않으면 만료 임박으로 처리
        }
    }

    public long getAccessTokenValidityInMilliseconds() {
        return accessTokenValidityInMilliseconds;
    }

    public long getRefreshTokenValidityInMilliseconds() {
        return refreshTokenValidityInMilliseconds;
    }
}