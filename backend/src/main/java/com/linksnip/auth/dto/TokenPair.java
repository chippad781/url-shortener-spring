package com.linksnip.auth.dto;

public record TokenPair(String accessToken, String refreshToken, String tokenType) {
    public static TokenPair of(String access, String refresh) {
        return new TokenPair(access, refresh, "Bearer");
    }
}
