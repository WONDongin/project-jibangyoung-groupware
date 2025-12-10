package com.jibangyoung.global.security;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.jibangyoung.domain.auth.AuthConstants;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String uri = request.getRequestURI();
        String method = request.getMethod();
        log.debug("[JWT FILTER] {} {}", method, uri);

        // 공개 경로는 토큰 검증 스킵
        if (isPublicUri(request)) {
            log.debug("[JWT FILTER] 공개 경로, 토큰 검증 스킵: {} {}", method, uri);
            filterChain.doFilter(request, response);
            return;
        }

        String token = resolveToken(request);

        try {
            if (StringUtils.hasText(token)) {
                // ✅ 토큰 유효성 검증 강화
                if (jwtTokenProvider.validateToken(token)) {
                    try {
                        Authentication authentication = jwtTokenProvider.getAuthentication(token);

                        if (authentication != null && authentication.isAuthenticated()) {
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                            log.debug("[JWT FILTER] 인증 성공: {}", authentication.getName());
                        } else {
                            log.warn("[JWT FILTER] Authentication 객체가 null이거나 인증되지 않음");
                            SecurityContextHolder.clearContext();
                        }
                    } catch (Exception authEx) {
                        log.warn("[JWT FILTER] Authentication 생성 실패: {}", authEx.getMessage());
                        SecurityContextHolder.clearContext();
                    }
                } else {
                    log.debug("[JWT FILTER] 토큰 유효성 검증 실패");
                    SecurityContextHolder.clearContext();
                }
            } else {
                log.debug("[JWT FILTER] 토큰 없음");
                SecurityContextHolder.clearContext();
            }
        } catch (Exception ex) {
            log.warn("[JWT FILTER] 토큰 처리 중 오류: {}", ex.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    /**
     * ✅ 공개 URI 판단 로직 개선
     */
    private boolean isPublicUri(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String method = request.getMethod();

        // OPTIONS 요청은 항상 허용 (CORS)
        if ("OPTIONS".equals(method)) {
            return true;
        }

        // 인증 관련 API는 모두 공개
        if (uri.startsWith("/api/auth/")) {
            return true;
        }

        // 공개 API
        if (uri.startsWith("/api/public/")) {
            return true;
        }

        // 커뮤니티 GET 요청만 공개
        if (uri.startsWith("/api/community/") && "GET".equals(method)) {
            return true;
        }

        // 정책 GET 요청 중 일부는 공개
        if (uri.startsWith("/api/policy/") && "GET".equals(method)) {
            // totalPolicies는 무조건 공개
            if (uri.contains("/totalPolicies")) {
                return true;
            }
            // 찜 관련과 추천 리스트를 제외한 나머지는 공개
            if (!uri.contains("/favorites") && !uri.contains("/recList")) {
                return true;
            }
        }

        // 대시보드는 모두 공개
        if (uri.startsWith("/api/dashboard/")) {
            return true;
        }

        // Swagger 관련 경로
        if (uri.startsWith("/swagger-ui/") || uri.startsWith("/v3/api-docs") ||
                uri.equals("/swagger-ui.html") || uri.startsWith("/webjars/")) {
            return true;
        }

        // Actuator health check
        if (uri.startsWith("/actuator/health")) {
            return true;
        }

        return false;
    }

    /**
     * ✅ 토큰 추출 로직 개선
     */
    private String resolveToken(HttpServletRequest request) {
        // 1. Authorization 헤더에서 Bearer 토큰 추출
        String bearerToken = request.getHeader(AuthConstants.AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(AuthConstants.TOKEN_PREFIX)) {
            String token = bearerToken.substring(AuthConstants.TOKEN_PREFIX.length()).trim();
            if (StringUtils.hasText(token)) {
                return token;
            }
        }

        // 2. 쿼리 파라미터에서 토큰 추출 (WebSocket 등을 위한 fallback)
        String queryToken = request.getParameter("token");
        if (StringUtils.hasText(queryToken)) {
            log.debug("[JWT FILTER] 쿼리 파라미터에서 토큰 추출");
            return queryToken.trim();
        }

        return null;
    }
}