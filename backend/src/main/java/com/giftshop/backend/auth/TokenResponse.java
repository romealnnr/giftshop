package com.giftshop.backend.auth;

import com.giftshop.backend.security.AccessToken;

public record TokenResponse(String accessToken, String tokenType, long expiresIn) {

    static TokenResponse from(AccessToken token) {
        return new TokenResponse(token.value(), "Bearer", token.expiresInSeconds());
    }

    @Override
    public String toString() {
        return "TokenResponse[accessToken=***, tokenType=" + tokenType + ", expiresIn=" + expiresIn + "]";
    }
}