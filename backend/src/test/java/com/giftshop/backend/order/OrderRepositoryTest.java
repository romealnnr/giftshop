package com.giftshop.backend.order;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.giftshop.backend.TestcontainersConfiguration;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class OrderRepositoryTest {

    @Autowired
    private OrderRepository repository;

    @Autowired
    private OrderJpaRepository jpaRepository;

    @BeforeEach
    void cleanDatabase() {
        jpaRepository.deleteAll();
    }

    private Order orderWithCode(String code) {
        return new Order(UUID.randomUUID(), 1L, "Carte Cinéma",
            new BigDecimal("25.00"), code, Instant.now());
    }

    @Test
    void saveIfCodeIsUnique_savesOrder_whenCodeIsNew() {
        Order order = orderWithCode("GIFT-AAAA-BBBB-CCCC");

        boolean saved = repository.saveIfCodeIsUnique(order);

        assertThat(saved).isTrue();
        assertThat(repository.findById(order.id()))
            .hasValueSatisfying(found -> {
                assertThat(found.code()).isEqualTo("GIFT-AAAA-BBBB-CCCC");
                assertThat(found.amount()).isEqualByComparingTo("25.00");
            });
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

    @Test
    void findById_returnsEmpty_whenOrderDoesNotExist() {
        assertThat(repository.findById(UUID.randomUUID())).isEmpty();
    }
}