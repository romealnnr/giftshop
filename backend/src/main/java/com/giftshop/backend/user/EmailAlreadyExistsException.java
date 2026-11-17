package com.giftshop.backend.user;

public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException(Throwable cause) {
        super("Email is already registered", cause);
    }
}
