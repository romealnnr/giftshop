package com.giftshop.backend.order;

import java.security.SecureRandom;

import org.springframework.stereotype.Component;

@Component
public class GiftCodeGenerator {

    // Constance
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final String PREFIX = "GIFT";
    private  static final int GROUPS = 3;
    private static final int GROUP_LENGHT = 4;
    
    private final SecureRandom random = new SecureRandom();

    // Generate a Gift-code
    public String generate(){
        StringBuilder code = new StringBuilder(PREFIX);
        for (int group = 0; group < GROUPS; group++){
            code.append('-');
            for (int i = 0; i < GROUP_LENGHT; i++){
                code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
            }
        }

        return code.toString();
    }
}