package com.giftshop.backend.redemption;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GiftCodeRequest(
    @NotBlank @Size(max = 30) String code
) {

    @Override
    public String toString() {
        return "GiftCodeRequest[code=***]";
    }
}
