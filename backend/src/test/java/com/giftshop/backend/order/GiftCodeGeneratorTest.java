package com.giftshop.backend.order;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

class GiftCodeGeneratorTest {

    private final GiftCodeGenerator generator = new GiftCodeGenerator();

    @Test
    void generate_returnsCodeWithExpectedFormat() {
        String code = generator.generate();

        assertThat(code).matches("GIFT-[A-HJ-NP-Z2-9]{4}-[A-HJ-NP-Z2-9]{4}-[A-HJ-NP-Z2-9]{4}");
    }

    @Test
    void generate_returnsDifferentCodes() {
        Set<String> codes = new HashSet<>();

        for (int i = 0; i < 1000; i++) {
            codes.add(generator.generate());
        }

        assertThat(codes).hasSize(1000);
    }
}