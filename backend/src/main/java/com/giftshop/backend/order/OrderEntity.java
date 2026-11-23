package com.giftshop.backend.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "orders")
class OrderEntity {

    @Id
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "gift_card_id", nullable = false)
    private Long giftCardId;

    @Column(name = "gift_card_name", nullable = false, length = 100)
    private String giftCardName;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal balance;

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected OrderEntity() {
        // required by JPA
    }

    static OrderEntity from(Order order) {
        OrderEntity entity = new OrderEntity();
        entity.id = order.id();
        entity.userId = order.userId();
        entity.giftCardId = order.giftCardId();
        entity.giftCardName = order.giftCardName();
        entity.amount = order.amount();
        entity.balance = order.balance();
        entity.code = order.code();
        entity.createdAt = order.createdAt();
        return entity;
    }

    Order toOrder() {
        return new Order(id, userId, giftCardId, giftCardName, amount, balance, code, createdAt);
    }
}
