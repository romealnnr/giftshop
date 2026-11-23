package com.giftshop.backend.redemption;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * Gift codes are bearer secrets: they always travel in the request body,
 * never in the URL, because URLs end up in browser histories and server logs.
 */
@RestController
@RequestMapping("/api/gift-codes")
public class GiftCodeController {

    private final RedemptionService redemptionService;

    public GiftCodeController(RedemptionService redemptionService) {
        this.redemptionService = redemptionService;
    }

    @PostMapping("/balance")
    public GiftCodeBalance balance(@Valid @RequestBody GiftCodeRequest request,
                                   @AuthenticationPrincipal Jwt jwt) {
        return redemptionService.getBalance(request.code(), currentUserId(jwt));
    }

    @PostMapping("/redeem")
    public RedemptionResult redeem(@Valid @RequestBody RedeemRequest request,
                                   @AuthenticationPrincipal Jwt jwt) {
        return redemptionService.redeem(request.code(), request.amount(), currentUserId(jwt));
    }

    private static UUID currentUserId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
