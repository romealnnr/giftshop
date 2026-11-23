package com.giftshop.backend.redemption;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import com.giftshop.backend.TestcontainersConfiguration;
import com.giftshop.backend.order.Order;
import com.giftshop.backend.order.OrderRepository;
import com.giftshop.backend.user.Role;
import com.giftshop.backend.user.User;
import com.giftshop.backend.user.UserRepository;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class RedemptionConcurrencyIntegrationTest {

    @Autowired
    private RedemptionService redemptionService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void concurrentRedemptions_neverSpendMoreThanTheBalance() throws Exception {
        // A user and a 30 EUR gift card
        User user = new User(UUID.randomUUID(), "race-" + UUID.randomUUID() + "@example.com",
            Role.USER, true, Instant.now());
        userRepository.save(user, "fake-hash-for-tests");
        String code = "RACE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Order card = new Order(UUID.randomUUID(), user.id(), 1L, "Carte Cinéma",
            new BigDecimal("30.00"), code, Instant.now());
        orderRepository.saveIfCodeIsUnique(card);

        // 10 threads try to spend 5 EUR each, all at the same moment
        int attempts = 10;
        ExecutorService pool = Executors.newFixedThreadPool(attempts);
        CountDownLatch startSignal = new CountDownLatch(1);
        AtomicInteger succeeded = new AtomicInteger();
        AtomicInteger refused = new AtomicInteger();
        List<Future<?>> results = new ArrayList<>();

        for (int i = 0; i < attempts; i++) {
            results.add(pool.submit(() -> {
                startSignal.await();
                try {
                    redemptionService.redeem(code, new BigDecimal("5.00"), user.id());
                    succeeded.incrementAndGet();
                } catch (InsufficientBalanceException e) {
                    refused.incrementAndGet();
                }
                return null;
            }));
        }

        startSignal.countDown();
        for (Future<?> result : results) {
            result.get(30, TimeUnit.SECONDS);
        }
        pool.shutdown();

        // Exactly 30 EUR spent: 6 redemptions accepted, 4 refused
        assertThat(succeeded.get()).isEqualTo(6);
        assertThat(refused.get()).isEqualTo(4);

        BigDecimal balance = jdbcTemplate.queryForObject(
            "SELECT balance FROM orders WHERE id = ?", BigDecimal.class, card.id());
        assertThat(balance).isEqualByComparingTo("0.00");

        // The ledger matches the balance: amount - sum(redemptions) = balance
        BigDecimal totalRedeemed = jdbcTemplate.queryForObject(
            "SELECT SUM(amount) FROM redemptions WHERE order_id = ?", BigDecimal.class, card.id());
        assertThat(totalRedeemed).isEqualByComparingTo("30.00");
    }
}
