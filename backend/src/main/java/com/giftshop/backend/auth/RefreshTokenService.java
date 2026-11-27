package com.giftshop.backend.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Issues, rotates and revokes refresh tokens.
 *
 * <p>A refresh token is a random 256-bit secret. Only its SHA-256 hash is stored:
 * a fast hash is enough for a random secret that cannot be guessed (unlike a
 * password), and a hash without salt can be looked up in the database.
 */
@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

    static final Duration TIME_TO_LIVE = Duration.ofDays(7);

    private final RefreshTokenJpaRepository repository;
    private final SecureRandom random = new SecureRandom();

    public RefreshTokenService(RefreshTokenJpaRepository repository) {
        this.repository = repository;
    }

    /**
     * Issues a new token in a new family, at login.
     *
     * @return the raw token, to send to the client (it is never stored)
     */
    public String issue(UUID userId) {
        return issueInFamily(userId, UUID.randomUUID());
    }

    /**
     * Exchanges a valid token for a new one in the same family, and revokes the old one.
     *
     * <p>If a token that was already revoked is used again, it was probably stolen:
     * the whole family is revoked, and both the thief and the user must log in again.
     *
     * <p>{@code noRollbackFor}: the family revocation must be committed even though an
     * exception is thrown right after it. By default, Spring would roll it back.
     *
     * @throws InvalidRefreshTokenException if the token is unknown, expired, revoked or reused
     */
    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public RotatedRefreshToken rotate(String rawToken) {
        RefreshTokenEntity current = repository.findByTokenHash(sha256(rawToken))
            .orElseThrow(InvalidRefreshTokenException::new);
        Instant now = Instant.now();

        if (current.isRevoked()) {
            repository.revokeFamily(current.familyId(), now);
            log.warn("Reuse of a revoked refresh token detected: the whole session family is revoked");
            throw new InvalidRefreshTokenException();
        }
        if (current.isExpired(now)) {
            throw new InvalidRefreshTokenException();
        }

        current.revoke(now);
        String newToken = issueInFamily(current.userId(), current.familyId());
        return new RotatedRefreshToken(current.userId(), newToken);
    }

    /**
     * Ends the session of this token (logout): its whole family is revoked.
     * An unknown token is ignored, so that logging out twice is not an error.
     */
    @Transactional
    public void revoke(String rawToken) {
        repository.findByTokenHash(sha256(rawToken))
            .ifPresent(token -> repository.revokeFamily(token.familyId(), Instant.now()));
    }

    private String issueInFamily(UUID userId, UUID familyId) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        repository.save(RefreshTokenEntity.create(
            userId, familyId, sha256(rawToken), Instant.now(), TIME_TO_LIVE));
        return rawToken;
    }

    static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is always available in Java", e);
        }
    }
}
