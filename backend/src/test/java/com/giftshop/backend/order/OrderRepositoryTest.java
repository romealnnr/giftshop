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

/**
 * The database is shared with other test classes: these tests never delete data,
 * they create their own users and unique gift codes instead.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class OrderRepositoryTest {

    @Autowired
    private OrderRepository repository;

    @Autowired
    private UserRepository userRepository;

    private UUID alice;
    private UUID bob;

    @BeforeEach
    void setUp() {
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

    private static String uniqueCode() {
        return "TEST-" + UUID.randomUUID().toString().substring(0, 13).toUpperCase();
    }

    private Order orderFor(UUID userId, String code, Instant createdAt) {
        return new Order(UUID.randomUUID(), userId, 1L, "Carte Cinéma",
            new BigDecimal("25.00"), code, createdAt);
    }

    @Test
    void saveIfCodeIsUnique_savesOrder_andItsOwnerCanFindIt() {
        String code = uniqueCode();
        Order order = orderFor(alice, code, Instant.now());

        boolean saved = repository.saveIfCodeIsUnique(order);

        assertThat(saved).isTrue();
        assertThat(repository.findByIdForUser(order.id(), alice))
            .hasValueSatisfying(found -> {
                assertThat(found.code()).isEqualTo(code);
                assertThat(found.userId()).isEqualTo(alice);
                assertThat(found.amount()).isEqualByComparingTo("25.00");
            });
    }

    @Test
    void saveIfCodeIsUnique_rejectsOrder_whenCodeAlreadyExists() {
        String code = uniqueCode();
        repository.saveIfCodeIsUnique(orderFor(alice, code, Instant.now()));
        Order duplicate = orderFor(bob, code, Instant.now());

        boolean saved = repository.saveIfCodeIsUnique(duplicate);

        assertThat(saved).isFalse();
        assertThat(repository.findByIdForUser(duplicate.id(), bob)).isEmpty();
    }

    @Test
    void findByIdForUser_returnsEmpty_forAnotherUser() {
        Order alicesOrder = orderFor(alice, uniqueCode(), Instant.now());
        repository.saveIfCodeIsUnique(alicesOrder);

        assertThat(repository.findByIdForUser(alicesOrder.id(), bob)).isEmpty();
    }

    @Test
    void findAllForUser_returnsOnlyTheUsersOrders_newestFirst() {
        String olderCode = uniqueCode();
        String newerCode = uniqueCode();
        repository.saveIfCodeIsUnique(
            orderFor(alice, olderCode, Instant.parse("2026-01-01T10:00:00Z")));
        repository.saveIfCodeIsUnique(
            orderFor(alice, newerCode, Instant.parse("2026-01-02T10:00:00Z")));
        repository.saveIfCodeIsUnique(
            orderFor(bob, uniqueCode(), Instant.now()));

        assertThat(repository.findAllForUser(alice))
            .extracting(Order::code)
            .containsExactly(newerCode, olderCode);
    }

    @Test
    void newOrder_isStoredWithItsFullBalance() {
        Order order = orderFor(alice, uniqueCode(), Instant.now());

        repository.saveIfCodeIsUnique(order);

        assertThat(repository.findByIdForUser(order.id(), alice))
            .hasValueSatisfying(found ->
                assertThat(found.balance()).isEqualByComparingTo("25.00"));
    }
}
