package com.giftshop.backend.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.JwtException;

import com.giftshop.backend.user.Role;
import com.giftshop.backend.user.User;

class TokenServiceTest {

    private final JwtProperties properties =
        new JwtProperties("giftshop", Duration.ofMinutes(15), null, null, true);

    private final JwtConfig jwtConfig = new JwtConfig();

    private KeyPair keyPair;
    private TokenService tokenService;
    private JwtDecoder decoder;

    private final User alice = new User(
        UUID.randomUUID(), "alice@example.com", Role.USER, true, Instant.now());

    @BeforeEach
    void setUp() throws Exception {
        keyPair = newKeyPair();
        tokenService = new TokenService(jwtConfig.jwtEncoder(keyPair), properties);
        decoder = jwtConfig.jwtDecoder(keyPair, properties, jwt -> OAuth2TokenValidatorResult.success());
    }

    private static KeyPair newKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    @Test
    void createAccessToken_containsUserIdRoleAndIssuer() {
        AccessToken token = tokenService.createAccessToken(alice);

        Jwt jwt = decoder.decode(token.value());

        assertThat(jwt.getSubject()).isEqualTo(alice.id().toString());
        assertThat(jwt.getClaimAsString("role")).isEqualTo("USER");
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("giftshop");
    }

    @Test
    void createAccessToken_expiresAfter15Minutes() {
        AccessToken token = tokenService.createAccessToken(alice);

        Jwt jwt = decoder.decode(token.value());

        assertThat(token.expiresInSeconds()).isEqualTo(900);
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt()))
            .isEqualTo(Duration.ofMinutes(15));
    }

    @Test
    void createAccessToken_neverContainsTheEmail() {
        AccessToken token = tokenService.createAccessToken(alice);

        Jwt jwt = decoder.decode(token.value());

        assertThat(jwt.getClaims().values())
            .noneMatch(value -> value.toString().contains("alice@example.com"));
    }

    @Test
    void decoder_rejectsTokenSignedWithAnotherKey() throws Exception {
        TokenService attacker = new TokenService(jwtConfig.jwtEncoder(newKeyPair()), properties);
        AccessToken forgedToken = attacker.createAccessToken(alice);

        assertThatThrownBy(() -> decoder.decode(forgedToken.value()))
            .isInstanceOf(JwtException.class);
    }

    @Test
    void decoder_rejectsTokenFromAnotherIssuer() {
        JwtProperties otherIssuer =
            new JwtProperties("evil-app", Duration.ofMinutes(15), null, null, true);
        TokenService otherApp = new TokenService(jwtConfig.jwtEncoder(keyPair), otherIssuer);
        AccessToken token = otherApp.createAccessToken(alice);

        assertThatThrownBy(() -> decoder.decode(token.value()))
            .isInstanceOf(JwtException.class);
    }
}