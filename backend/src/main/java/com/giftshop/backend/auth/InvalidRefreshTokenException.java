package com.giftshop.backend.auth;

/** The refresh token is unknown, expired, revoked or was reused. */
public class InvalidRefreshTokenException extends RuntimeException {

    public InvalidRefreshTokenException() {
        super("Session expired. Please log in again.");
    }
}
