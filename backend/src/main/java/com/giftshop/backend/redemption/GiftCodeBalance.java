package com.giftshop.backend.redemption;

import java.math.BigDecimal;

/** The code itself is deliberately not sent back: the client already has it. */
public record GiftCodeBalance(String giftCardName, BigDecimal amount, BigDecimal balance) {
}
