package com.giftshop.backend.giftcard;

import java.io.Serializable;
import java.math.BigDecimal;

/** Serializable so that the catalog can be stored in the Redis cache. */
public record GiftCard(Long id, String name, String category, BigDecimal amount)
        implements Serializable {
}
