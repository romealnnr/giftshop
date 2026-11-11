package com.giftshop.backend.order;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateOrderRequest(@NotNull @Positive Long giftCardId) {
//@NotNull: This field is required.
//@Positive: The identifier must be greater than 0.
}