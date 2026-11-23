package com.giftshop.backend.redemption;

import java.time.Duration;
import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.giftshop.backend.security.RateLimiter;

import jakarta.validation.Valid;

/**
 * Gift codes are bearer secrets: they always travel in the request body,
 * never in the URL, because URLs end up in browser histories and server logs.
 */
@RestController
@RequestMapping("/api/gift-codes")
public class GiftCodeController {

    /** Against code guessing: at most 10 gift code operations per minute and per user. */
    static final int GIFT_CODE_ATTEMPTS_PER_MINUTE = 10;

    private final RedemptionService redemptionService;
    private final RateLimiter rateLimiter;

    public GiftCodeController(RedemptionService redemptionService, RateLimiter rateLimiter) {
        this.redemptionService = redemptionService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/balance")
    public GiftCodeBalance balance(@Valid @RequestBody GiftCodeRequest request,
                                   @AuthenticationPrincipal Jwt jwt) {
        UUID userId = currentUserId(jwt);
        checkRateLimit(userId);
        return redemptionService.getBalance(request.code(), userId);
    }

    @PostMapping("/redeem")
    public RedemptionResult redeem(@Valid @RequestBody RedeemRequest request,
                                   @AuthenticationPrincipal Jwt jwt) {
        UUID userId = currentUserId(jwt);
        checkRateLimit(userId);
        return redemptionService.redeem(request.code(), request.amount(), userId);
    }

    /** Checking and redeeming share the same counter: alternating does not give more attempts. */
    private void checkRateLimit(UUID userId) {
        rateLimiter.check("gift-code", userId.toString(),
            GIFT_CODE_ATTEMPTS_PER_MINUTE, Duration.ofMinutes(1));
    }

    private static UUID currentUserId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
