package com.giftshop.backend.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record Order(
    UUID id,
    Long giftCardId,
    String giftCardName,
    BigDecimal amount,
    String code,
    Instant createdAt
) {
}