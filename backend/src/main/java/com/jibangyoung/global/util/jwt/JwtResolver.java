package com.jibangyoung.global.util.jwt;

public class JwtResolver {
    public static String extractBearerToken(String header) {
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        throw new IllegalArgumentException("Invalid Authorization header");
    }
}
