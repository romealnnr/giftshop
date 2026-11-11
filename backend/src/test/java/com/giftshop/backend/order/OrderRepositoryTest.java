package com.giftshop.backend.order;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class OrderRepositoryTest {

    private final OrderRepository repository = new OrderRepository();

    private Order orderWithCode(String code) {
        return new Order(UUID.randomUUID(), 1L, "Carte Cinéma",
            new BigDecimal("25.00"), code, Instant.now());
    }

    @Test
    void saveIfCodeIsUnique_savesOrder_whenCodeIsNew() {
        Order order = orderWithCode("GIFT-AAAA-BBBB-CCCC");

        boolean saved = repository.saveIfCodeIsUnique(order);

        assertThat(saved).isTrue();
        assertThat(repository.findById(order.id())).contains(order);
    }

    @Test
    void saveIfCodeIsUnique_rejectsOrder_whenCodeAlreadyExists() {
        Order first = orderWithCode("GIFT-AAAA-BBBB-CCCC");
        Order duplicate = orderWithCode("GIFT-AAAA-BBBB-CCCC");
        repository.saveIfCodeIsUnique(first);

        boolean saved = repository.saveIfCodeIsUnique(duplicate);

        assertThat(saved).isFalse();
        assertThat(repository.findById(duplicate.id())).isEmpty();
    }
}