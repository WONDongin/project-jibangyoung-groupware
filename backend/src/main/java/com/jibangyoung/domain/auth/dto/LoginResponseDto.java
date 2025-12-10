// backend/src/main/java/com/jibangyoung/domain/auth/dto/LoginResponseDto.java
package com.jibangyoung.domain.auth.dto;

import com.jibangyoung.domain.auth.entity.User;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDto {
    private UserDto user;
    private String tokenType;
    private String accessToken;
    private String refreshToken;
    private Long expiresIn;
    private String issuedAt;
    private String expiresAt;

    public static LoginResponseDto of(User user, String accessToken, String refreshToken,
Long expiresIn, String issuedAt, String expiresAt) {
        return LoginResponseDto.builder()
                .user(UserDto.from(user))
                .tokenType("Bearer")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(expiresIn)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .build();
    }
}

