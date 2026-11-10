package com.giftshop.backend.giftcard;

import java.math.BigDecimal;

// Create the giftcad object
public record GiftCard( Long id, String name, String category, BigDecimal amount) {
} 
