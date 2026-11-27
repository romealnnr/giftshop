package com.giftshop.backend.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "refresh_tokens")
class RefreshTokenEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "family_id", nullable = false)
    private UUID familyId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected RefreshTokenEntity() {
        // required by JPA
    }

    static RefreshTokenEntity create(UUID userId, UUID familyId, String tokenHash,
                                     Instant now, Duration timeToLive) {
        RefreshTokenEntity entity = new RefreshTokenEntity();
        entity.id = UUID.randomUUID();
        entity.userId = userId;
        entity.familyId = familyId;
        entity.tokenHash = tokenHash;
        entity.createdAt = now;
        entity.expiresAt = now.plus(timeToLive);
        return entity;
    }

    UUID userId() {
        return userId;
    }

    UUID familyId() {
        return familyId;
    }

    boolean isRevoked() {
        return revokedAt != null;
    }

    boolean isExpired(Instant now) {
        return !expiresAt.isAfter(now);
    }

    void revoke(Instant now) {
        revokedAt = now;
    }
}
