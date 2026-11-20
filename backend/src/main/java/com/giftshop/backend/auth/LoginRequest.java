package com.giftshop.backend.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
    @NotBlank @Size(max = 254) String email,
    @NotBlank @Size(max = 72) String password
) {

    public LoginRequest {
        email = (email == null) ? null : email.trim();
    }

    @Override
    public String toString() {
        return "LoginRequest[email=***, password=***]";
    }
}