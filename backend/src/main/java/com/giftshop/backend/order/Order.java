package com.giftshop.backend.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A purchased gift card.
 *
 * @param amount  the value of the card when it was bought
 * @param balance what remains to be spent on it
 */
public record Order(
    UUID id,
    UUID userId,
    Long giftCardId,
    String giftCardName,
    BigDecimal amount,
    BigDecimal balance,
    String code,
    Instant createdAt
) {

    /** A new gift card: nothing has been spent yet, so its balance is its full amount. */
    public Order(UUID id, UUID userId, Long giftCardId, String giftCardName,
                 BigDecimal amount, String code, Instant createdAt) {
        this(id, userId, giftCardId, giftCardName, amount, amount, code, createdAt);
    }
}
