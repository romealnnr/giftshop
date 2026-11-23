package com.giftshop.backend.security;

import java.util.UUID;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import com.giftshop.backend.user.UserService;

/**
 * Rejects the tokens of accounts that were deleted or disabled after the token
 * was issued, on every route. Runs after the signature has been verified, so
 * forged tokens never reach the database.
 */
@Component
public class ActiveUserTokenValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error INACTIVE_ACCOUNT = new OAuth2Error(
        OAuth2ErrorCodes.INVALID_TOKEN, "Account is no longer active", null);

    private final UserService userService;

    public ActiveUserTokenValidator(UserService userService) {
        this.userService = userService;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        UUID userId;
        try {
            userId = UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException | NullPointerException e) {
            return OAuth2TokenValidatorResult.failure(INACTIVE_ACCOUNT);
        }

        return userService.isActive(userId)
            ? OAuth2TokenValidatorResult.success()
            : OAuth2TokenValidatorResult.failure(INACTIVE_ACCOUNT);
    }
}
