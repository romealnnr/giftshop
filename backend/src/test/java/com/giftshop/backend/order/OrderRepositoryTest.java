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
import com.giftshop.backend.user.Role;
import com.giftshop.backend.user.User;
import com.giftshop.backend.user.UserRepository;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class OrderRepositoryTest {

    @Autowired
    private OrderRepository repository;

    @Autowired
    private OrderJpaRepository jpaRepository;

    @Autowired
    private UserRepository userRepository;

    private UUID alice;
    private UUID bob;

    @BeforeEach
    void setUp() {
        jpaRepository.deleteAll();
        alice = newUser();
        bob = newUser();
    }

    /** Orders reference users (foreign key): the buyer must exist in the database. */
    private UUID newUser() {
        User user = new User(UUID.randomUUID(), "user-" + UUID.randomUUID() + "@example.com",
            Role.USER, true, Instant.now());
        userRepository.save(user, "fake-hash-for-tests");
        return user.id();
    }

    private Order orderFor(UUID userId, String code, Instant createdAt) {
        return new Order(UUID.randomUUID(), userId, 1L, "Carte Cinéma",
            new BigDecimal("25.00"), code, createdAt);
    }

    @Test
    void saveIfCodeIsUnique_savesOrder_andItsOwnerCanFindIt() {
        Order order = orderFor(alice, "GIFT-AAAA-BBBB-CCCC", Instant.now());

        boolean saved = repository.saveIfCodeIsUnique(order);

        assertThat(saved).isTrue();
        assertThat(repository.findByIdForUser(order.id(), alice))
            .hasValueSatisfying(found -> {
                assertThat(found.code()).isEqualTo("GIFT-AAAA-BBBB-CCCC");
                assertThat(found.userId()).isEqualTo(alice);
                assertThat(found.amount()).isEqualByComparingTo("25.00");
            });
    }

    @Test
    void saveIfCodeIsUnique_rejectsOrder_whenCodeAlreadyExists() {
        repository.saveIfCodeIsUnique(orderFor(alice, "GIFT-AAAA-BBBB-CCCC", Instant.now()));
        Order duplicate = orderFor(bob, "GIFT-AAAA-BBBB-CCCC", Instant.now());

        boolean saved = repository.saveIfCodeIsUnique(duplicate);

        assertThat(saved).isFalse();
        assertThat(repository.findByIdForUser(duplicate.id(), bob)).isEmpty();
    }

    @Test
    void findByIdForUser_returnsEmpty_forAnotherUser() {
        Order alicesOrder = orderFor(alice, "GIFT-AAAA-BBBB-CCCC", Instant.now());
        repository.saveIfCodeIsUnique(alicesOrder);

        assertThat(repository.findByIdForUser(alicesOrder.id(), bob)).isEmpty();
    }

    @Test
    void findAllForUser_returnsOnlyTheUsersOrders_newestFirst() {
        repository.saveIfCodeIsUnique(
            orderFor(alice, "GIFT-OLDE-OLDE-OLDE", Instant.parse("2026-01-01T10:00:00Z")));
        repository.saveIfCodeIsUnique(
            orderFor(alice, "GIFT-NEWE-NEWE-NEWE", Instant.parse("2026-01-02T10:00:00Z")));
        repository.saveIfCodeIsUnique(
            orderFor(bob, "GIFT-BOBB-BOBB-BOBB", Instant.now()));

        assertThat(repository.findAllForUser(alice))
            .extracting(Order::code)
            .containsExactly("GIFT-NEWE-NEWE-NEWE", "GIFT-OLDE-OLDE-OLDE");
    }
}
