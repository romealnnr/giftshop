package com.giftshop.backend.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank @Email @Size(max = 254) String email,
    @NotBlank @Size(min = 12, max = 72) String password
) {

    public RegisterRequest {
        email = (email == null) ? null : email.trim();
    }

    @Override
    public String toString() {
        return "RegisterRequest[email=***, password=***]";
    }
}