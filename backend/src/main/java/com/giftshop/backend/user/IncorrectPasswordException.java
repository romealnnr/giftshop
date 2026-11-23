package com.giftshop.backend.user;

/** The password given to confirm a sensitive action is wrong. */
public class IncorrectPasswordException extends RuntimeException {

    public IncorrectPasswordException() {
        super("Password is incorrect");
    }
}
