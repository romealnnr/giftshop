package com.giftshop.backend.redemption;

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
import com.giftshop.backend.order.Order;
import com.giftshop.backend.order.OrderRepository;
import com.giftshop.backend.user.Role;
import com.giftshop.backend.user.User;
import com.giftshop.backend.user.UserRepository;

/**
 * The database is shared with other test classes: these tests never delete data,
 * they create their own users and gift cards instead.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class RedemptionRepositoryTest {

    @Autowired
    private RedemptionRepository repository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    private UUID alice;
    private UUID bob;
    private UUID cardId;

    @BeforeEach
    void setUp() {
        alice = newUser();
        bob = newUser();

        // A gift card bought by Bob
        Order card = new Order(UUID.randomUUID(), bob, 3L, "Carte Restaurant", new BigDecimal("50.00"),
            "TEST-" + UUID.randomUUID().toString().substring(0, 13).toUpperCase(), Instant.now());
        orderRepository.saveIfCodeIsUnique(card);
        cardId = card.id();
    }

    private UUID newUser() {
        User user = new User(UUID.randomUUID(), "user-" + UUID.randomUUID() + "@example.com",
            Role.USER, true, Instant.now());
        userRepository.save(user, "fake-hash-for-tests");
        return user.id();
    }

    private Redemption redemption(UUID userId, String amount, Instant createdAt) {
        return new Redemption(UUID.randomUUID(), cardId, userId, new BigDecimal(amount), createdAt);
    }

    @Test
    void findAllForUser_returnsOnlyTheUsersRedemptions_newestFirst() {
        repository.save(redemption(alice, "10.00", Instant.parse("2026-01-01T10:00:00Z")));
        repository.save(redemption(alice, "15.00", Instant.parse("2026-01-02T10:00:00Z")));
        repository.save(redemption(bob, "5.00", Instant.now()));

        assertThat(repository.findAllForUser(alice))
            .extracting(Redemption::amount)
            .usingElementComparator(BigDecimal::compareTo)
            .containsExactly(new BigDecimal("15.00"), new BigDecimal("10.00"));
    }

    @Test
    void findAllForUser_returnsEmptyList_forUserWithoutRedemptions() {
        assertThat(repository.findAllForUser(newUser())).isEmpty();
    }
}
