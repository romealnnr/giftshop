package com.giftshop.backend.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import com.giftshop.backend.auth.AccountDisabledException;
import com.giftshop.backend.auth.InvalidCredentialsException;
import com.giftshop.backend.giftcard.GiftCardNotFoundException;
import com.giftshop.backend.order.GiftCodeGenerationException;
import com.giftshop.backend.order.OrderNotFoundException;
import com.giftshop.backend.redemption.InsufficientBalanceException;
import com.giftshop.backend.redemption.InvalidGiftCodeException;import com.giftshop.backend.user.EmailAlreadyExistsException;
import com.giftshop.backend.user.InvalidPasswordException;
import com.giftshop.backend.auth.AccountDisabledException;
import com.giftshop.backend.auth.InvalidCredentialsException;

import com.giftshop.backend.user.InactiveAccountException;
import com.giftshop.backend.user.IncorrectPasswordException;
import com.giftshop.backend.redemption.InsufficientBalanceException;
import com.giftshop.backend.redemption.InvalidGiftCodeException;
import com.giftshop.backend.user.InvalidPasswordException;
import com.giftshop.backend.user.UserNotFoundException;
/**
 * Turns business exceptions into HTTP responses (RFC 9457 problem details).
 * Standard Spring MVC errors (validation, malformed JSON, unknown route...)
 * are handled by {@link ResponseEntityExceptionHandler}.
 */@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
 // ---------- gift cards and orders ----------
    @ExceptionHandler(GiftCardNotFoundException.class)
    public ProblemDetail handleNotFound(GiftCardNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(GiftCodeGenerationException.class)
    public ProblemDetail handleCodeGeneration(GiftCodeGenerationException ex) {
        log.error("Gift code generation failed", ex);
        return ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "The order could not be created. Please try again later.");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "An unexpected error occurred.");
    }

    @ExceptionHandler(OrderNotFoundException.class)
    public ProblemDetail handleOrderNotFound(OrderNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ProblemDetail handleEmailAlreadyExists(EmailAlreadyExistsException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Email is already registered");
    }

    @ExceptionHandler(InvalidPasswordException.class)
    public ProblemDetail handleInvalidPassword(InvalidPasswordException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ProblemDetail handleInvalidCredentials(InvalidCredentialsException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(AccountDisabledException.class)
    public ProblemDetail handleAccountDisabled(AccountDisabledException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ProblemDetail handleUserNotFound(UserNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

        @ExceptionHandler(InactiveAccountException.class)
    public ProblemDetail handleInactiveAccount(InactiveAccountException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(IncorrectPasswordException.class)
    public ProblemDetail handleIncorrectPassword(IncorrectPasswordException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(InvalidGiftCodeException.class)
    public ProblemDetail handleInvalidGiftCode(InvalidGiftCodeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public ProblemDetail handleInsufficientBalance(InsufficientBalanceException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }
}
