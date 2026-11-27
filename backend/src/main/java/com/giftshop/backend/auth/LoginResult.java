package com.giftshop.backend.auth;

import com.giftshop.backend.security.AccessToken;

/**
 * The two tokens of a session: the short-lived access token (sent in the body)
 * and the long-lived refresh token (sent in an HttpOnly cookie).
 */
public record LoginResult(AccessToken accessToken, String refreshToken) {

    @Override
    public String toString() {
        return "LoginResult[accessToken=***, refreshToken=***]";
    }
}
