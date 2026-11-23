package com.giftshop.backend.redemption;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** One use of a gift card: an entry of the ledger. */
public record Redemption(
    UUID id,
    UUID orderId,
    UUID userId,
    BigDecimal amount,
    Instant createdAt
) {
}
