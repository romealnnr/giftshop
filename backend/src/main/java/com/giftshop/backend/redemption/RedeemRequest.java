package com.giftshop.backend.redemption;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RedeemRequest(
    @NotBlank @Size(max = 30) String code,
    @NotNull @DecimalMin("0.01") @Digits(integer = 8, fraction = 2) BigDecimal amount
) {

    @Override
    public String toString() {
        return "RedeemRequest[code=***, amount=" + amount + "]";
    }
}
