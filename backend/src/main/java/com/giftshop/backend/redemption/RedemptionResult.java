package com.giftshop.backend.redemption;

import java.math.BigDecimal;

public record RedemptionResult(BigDecimal redeemedAmount, BigDecimal remainingBalance) {
}
