package com.giftshop.backend.user;

/** The account was deleted or disabled after the access token was issued. */
public class InactiveAccountException extends RuntimeException {

    public InactiveAccountException() {
        super("Account is no longer active");
    }
}
