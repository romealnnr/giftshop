package com.giftshop.backend.security;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

    private static final Logger log = LoggerFactory.getLogger(JwtConfig.class);

    @Bean
    KeyPair jwtKeyPair(JwtProperties properties, ResourceLoader resourceLoader)
            throws IOException, NoSuchAlgorithmException {
        if (properties.generateEphemeralKeys()) {
            log.warn("Using a temporary JWT key pair: tokens will not survive a restart");
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        }

        RSAPublicKey publicKey;
        try (InputStream in = resourceLoader.getResource(properties.publicKeyLocation()).getInputStream()) {
            publicKey = RsaKeyConverters.x509().convert(in);
        }
        RSAPrivateKey privateKey;
        try (InputStream in = resourceLoader.getResource(properties.privateKeyLocation()).getInputStream()) {
            privateKey = RsaKeyConverters.pkcs8().convert(in);
        }
        return new KeyPair(publicKey, privateKey);
    }

    @Bean
    JwtEncoder jwtEncoder(KeyPair jwtKeyPair) {
        RSAKey key = new RSAKey.Builder((RSAPublicKey) jwtKeyPair.getPublic())
            .privateKey(jwtKeyPair.getPrivate())
            .build();
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(key)));
    }

    /**
     * Verifies, in this order: the signature, then the expiry and the issuer,
     * then that the account is still active.
     */
    @Bean
    JwtDecoder jwtDecoder(KeyPair jwtKeyPair,
                          JwtProperties properties,
                          OAuth2TokenValidator<Jwt> activeUserTokenValidator) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder
            .withPublicKey((RSAPublicKey) jwtKeyPair.getPublic())
            .build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
            JwtValidators.createDefaultWithIssuer(properties.issuer()),
            activeUserTokenValidator));
        return decoder;
    }
}
