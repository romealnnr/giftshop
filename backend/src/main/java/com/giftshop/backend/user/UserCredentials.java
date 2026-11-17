package com.giftshop.backend.user;

public record UserCredentials(User user, String passwordHash) {
}