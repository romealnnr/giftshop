package com.giftshop.backend.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.giftshop.backend.giftcard.GiftCardNotFoundException;

@RestControllerAdvice 
public class GlobalExceptionHandler {
    
    @ExceptionHandler(GiftCardNotFoundException.class)
    public  ProblemDetail handleNotFound(GiftCardNotFoundException ex){
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    } 
}
