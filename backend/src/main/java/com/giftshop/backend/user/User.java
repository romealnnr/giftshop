package com.giftshop.backend.user;

import java.time.Instant;
import java.util.UUID;

public record User(UUID id, String email, Role role, boolean enabled, Instant createdAt) {
}