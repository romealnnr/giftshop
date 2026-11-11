package com.giftshop.backend.order;

public class GiftCodeGenerationException extends RuntimeException {

    public GiftCodeGenerationException(int attempts) {
        super("Could not generate a unique gift code after " + attempts + " attempts");
    }
}