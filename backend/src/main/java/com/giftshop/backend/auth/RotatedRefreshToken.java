package com.giftshop.backend.auth;

import java.util.UUID;

/** The result of a rotation: whose session it is, and the new raw token to send back. */
public record RotatedRefreshToken(UUID userId, String token) {

    @Override
    public String toString() {
        return "RotatedRefreshToken[userId=" + userId + ", token=***]";
    }
}
