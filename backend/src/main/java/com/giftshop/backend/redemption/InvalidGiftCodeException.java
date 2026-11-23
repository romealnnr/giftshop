package com.giftshop.backend.redemption;

public class InvalidGiftCodeException extends RuntimeException {

    public InvalidGiftCodeException() {
        super("Invalid gift code");
    }
}
