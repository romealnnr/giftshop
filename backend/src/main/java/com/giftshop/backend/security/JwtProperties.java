package com.giftshop.backend.security;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
    String issuer,
    Duration accessTokenTtl,
    String privateKeyLocation,
    String publicKeyLocation,
    boolean generateEphemeralKeys
) {
}