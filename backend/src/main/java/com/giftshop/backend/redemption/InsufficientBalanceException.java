package com.giftshop.backend.redemption;

public class InsufficientBalanceException extends RuntimeException {

    public InsufficientBalanceException() {
        super("Insufficient balance");
    }
}
