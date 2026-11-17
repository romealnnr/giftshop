package com.giftshop.backend.user;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
class UserEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserEntity() {
        // required by JPA
    }

    static UserEntity from(User user, String passwordHash) {
        UserEntity entity = new UserEntity();
        entity.id = user.id();
        entity.email = user.email();
        entity.passwordHash = passwordHash;
        entity.role = user.role();
        entity.enabled = user.enabled();
        entity.createdAt = user.createdAt();
        entity.updatedAt = user.createdAt();
        return entity;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    User toUser() {
        return new User(id, email, role, enabled, createdAt);
    }

    UserCredentials toCredentials() {
        return new UserCredentials(toUser(), passwordHash);
    }
}