package com.giftshop.backend.redemption;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "redemptions")
class RedemptionEntity {

    @Id
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected RedemptionEntity() {
        // required by JPA
    }

    static RedemptionEntity from(Redemption redemption) {
        RedemptionEntity entity = new RedemptionEntity();
        entity.id = redemption.id();
        entity.orderId = redemption.orderId();
        entity.userId = redemption.userId();
        entity.amount = redemption.amount();
        entity.createdAt = redemption.createdAt();
        return entity;
    }
}
