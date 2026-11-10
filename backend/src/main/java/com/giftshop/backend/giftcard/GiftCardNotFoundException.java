package com.giftshop.backend.giftcard;

public class GiftCardNotFoundException extends RuntimeException{
    
    public GiftCardNotFoundException(Long id){
        super("giftcard not found : " + id);
    }

    
}
