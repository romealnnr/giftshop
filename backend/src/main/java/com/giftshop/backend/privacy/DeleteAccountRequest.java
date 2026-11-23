package com.giftshop.backend.privacy;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeleteAccountRequest(
    @NotBlank @Size(max = 72) String password
) {

    @Override
    public String toString() {
        return "DeleteAccountRequest[password=***]";
    }
}
