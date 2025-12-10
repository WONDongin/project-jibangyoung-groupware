package com.jibangyoung.domain.auth;

public final class AuthConstants {
    private AuthConstants() {}
  
    public static final int MAX_REFRESH_TOKENS_PER_USER = 5; // 동시 로그인 

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String REFRESH_TOKEN_HEADER = "Refresh-Token";

    // === [비밀번호 정책 공통 상수] ===
    public static final int PASSWORD_MIN_LENGTH = 8;
    public static final int PASSWORD_MAX_LENGTH = 100;
    public static final String PASSWORD_REGEX = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\\\d)(?=.*[@$!%*?&])[A-Za-z\\\\d@$!%*?&]+$";
    public static final String PASSWORD_MESSAGE = "비밀번호는 8~100자, 대소문자/숫자/특수문자를 모두 포함해야 합니다.";

    public static final int USERNAME_MIN_LENGTH = 4;
    public static final int USERNAME_MAX_LENGTH = 50;

    public static final String DEFAULT_PROFILE_IMAGE = "/images/default-profile.png";

    // Error Messages – 운영 메시지 일관화
    public static final String USER_NOT_FOUND = "USER_NOT_FOUND";
    public static final String INVALID_PASSWORD = "INVALID_PASSWORD";
    public static final String ACCOUNT_LOCKED = "ACCOUNT_LOCKED";
    public static final String INVALID_TOKEN = "인증 정보가 유효하지 않습니다.";
    public static final String EXPIRED_TOKEN = "인증 정보가 유효하지 않습니다.";
    public static final String INVALID_REFRESH_TOKEN = "인증 정보가 유효하지 않습니다.";
    public static final String EXPIRED_REFRESH_TOKEN = "인증 정보가 유효하지 않습니다.";
    public static final String USERNAME_ALREADY_EXISTS = "USERNAME_ALREADY_EXISTS";
    public static final String EMAIL_ALREADY_EXISTS = "EMAIL_ALREADY_EXISTS";
}

