package com.jibangyoung.global.config;

import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.jibangyoung.global.security.CustomUserDetailsService;
import com.jibangyoung.global.security.JwtAuthenticationEntryPoint;
import com.jibangyoung.global.security.JwtAuthenticationFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

        private final CustomUserDetailsService customUserDetailsService;
        private final JwtAuthenticationFilter jwtAuthenticationFilter;
        private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        /**
         * AuthenticationProvider를 명시적으로 등록해 CustomUserDetailsService와 PasswordEncoder를
         * 사용합니다.
         */
        @Bean
        public AuthenticationProvider authenticationProvider() {
                DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
                provider.setUserDetailsService(customUserDetailsService);
                provider.setPasswordEncoder(passwordEncoder());
                return provider;
        }

        /**
         * AuthenticationConfiguration을 통해 AuthenticationManager를 노출합니다.
         */
        @Bean
        public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
                return configuration.getAuthenticationManager();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration config = new CorsConfiguration();
                config.setAllowedOrigins(Arrays.asList(
                                "http://localhost:3000",
                                "https://jibangyoung.kr"));
                config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
                config.setAllowedHeaders(Arrays.asList("*"));
                config.setExposedHeaders(Arrays.asList("Authorization", "Refresh-Token"));
                config.setAllowCredentials(true);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", config);
                return source;
        }

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                                // CORS는 빈에 등록된 CorsConfigurationSource를 사용하여 기본 활성화
                                .cors(Customizer.withDefaults())
                                .csrf(csrf -> csrf.disable())
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                // JwtAuthenticationEntryPoint 설정
                                .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))
                                // DaoAuthenticationProvider 등록
                                .authenticationProvider(authenticationProvider())
                                .authorizeHttpRequests(auth -> auth
                                                // OPTIONS 요청은 모두 허용
                                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                                                // 인증 관련 API는 모두 공개
                                                .requestMatchers("/api/auth/**").permitAll()
                                                // 로그아웃은 인증 유무와 관계없이 허용(명시적 추가)
                                                .requestMatchers(HttpMethod.POST, "/api/auth/logout").permitAll()

                                                // 공개 API
                                                .requestMatchers("/api/public/**").permitAll()

                                                // 커뮤니티 - 조회는 공개, 작성/수정/삭제는 인증 필요
                                                .requestMatchers(HttpMethod.GET, "/api/community/**").permitAll()
                                                .requestMatchers(HttpMethod.POST, "/api/community/write")
                                                .authenticated()
                                                .requestMatchers(HttpMethod.POST, "/api/community/posts/*/comments")
                                                .authenticated()
                                                .requestMatchers(HttpMethod.DELETE, "/api/community/comments/*")
                                                .authenticated()
                                                .requestMatchers(HttpMethod.POST, "/api/community/post/*/recommend")
                                                .authenticated()

                                                // 정책 - 대부분 공개, 찜 관련은 인증 필요
                                                .requestMatchers(HttpMethod.GET, "/api/policy/policy.c").permitAll()
                                                .requestMatchers(HttpMethod.GET, "/api/policy/totalPolicies")
                                                .permitAll() // totalPolicies 공개 허용
                                                .requestMatchers(HttpMethod.GET, "/api/policy/**").permitAll()
                                                .requestMatchers("/api/policy/sync").authenticated()
                                                .requestMatchers("/api/policy/favorites/**").authenticated()
                                                .requestMatchers("/api/policy/recList").authenticated()

                                                // 설문 - 인증 필요
                                                .requestMatchers("/api/survey/**").authenticated()

                                                // 추천 - 인증 필요
                                                .requestMatchers("/api/recommendation/**").authenticated()

                                                // 대시보드 - 공개
                                                .requestMatchers("/api/dashboard/**").permitAll()

                                                // 관리자 API - ADMIN 권한 필요 (메소드 레벨에서 체크)
                                                .requestMatchers("/api/admin/**").authenticated()

                                                // 멘토 API - 인증 필요 (권한은 메소드 레벨에서 체크)
                                                .requestMatchers("/api/mentor/**").authenticated()

                                                // 사용자 API - 인증 필요
                                                .requestMatchers("/api/users/**").authenticated()

                                                // 나머지 모든 요청은 인증 필요
                                                .anyRequest().authenticated())
                                // JWT 필터 등록
                                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }
}