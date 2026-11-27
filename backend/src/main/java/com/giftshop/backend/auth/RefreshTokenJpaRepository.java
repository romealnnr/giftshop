package com.giftshop.backend.auth;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenEntity, UUID> {

    /**
     * Locks the row (SELECT ... FOR UPDATE): two concurrent refreshes with the same
     * token are handled one after the other, so a token can only be rotated once.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

    @Modifying
    @Query("""
        UPDATE RefreshTokenEntity t
        SET t.revokedAt = :now
        WHERE t.familyId = :familyId AND t.revokedAt IS NULL
        """)
    int revokeFamily(@Param("familyId") UUID familyId, @Param("now") Instant now);
}
