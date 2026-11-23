package com.giftshop.backend.redemption;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.giftshop.backend.order.Order;
import com.giftshop.backend.order.OrderRepository;
import com.giftshop.backend.user.UserService;

@Service
public class RedemptionService {

    private final OrderRepository orderRepository;
    private final RedemptionRepository redemptionRepository;
    private final UserService userService;

    public RedemptionService(OrderRepository orderRepository,
                             RedemptionRepository redemptionRepository,
                             UserService userService) {
        this.orderRepository = orderRepository;
        this.redemptionRepository = redemptionRepository;
        this.userService = userService;
    }

    /**
     * Spends part of a gift card.
     *
     * <h2>Why this method IS {@code @Transactional}</h2>
     *
     * <p>Two writes must succeed or fail together: debiting the balance and recording
     * the redemption in the ledger. Without a transaction, a crash between the two
     * would leave money spent with no trace of it.
     *
     * <h2>How concurrent redemptions are handled</h2>
     *
     * <p>The balance is checked and debited by a single conditional statement
     * ({@code UPDATE ... WHERE balance >= amount}). PostgreSQL locks the row until the
     * transaction ends: a concurrent redemption of the same card waits, then checks
     * the condition again against the new balance. The balance can never go below zero.
     *
     * <p>A refused debit simply updates 0 rows: no SQL error happens, so the
     * transaction stays healthy (unlike the code collisions in {@code OrderService}).
     *
     * @throws com.giftshop.backend.user.InactiveAccountException if the account was deleted or disabled
     * @throws InvalidGiftCodeException if no gift card has this code
     * @throws InsufficientBalanceException if the balance is lower than the amount
     */
    @Transactional
    public RedemptionResult redeem(String code, BigDecimal amount, UUID userId) {
        userService.requireActiveUser(userId);

        Order card = orderRepository.findByGiftCode(normalizeCode(code))
            .orElseThrow(InvalidGiftCodeException::new);

        if (!orderRepository.debitBalance(card.id(), amount)) {
            throw new InsufficientBalanceException();
        }

        redemptionRepository.save(
            new Redemption(UUID.randomUUID(), card.id(), userId, amount, Instant.now()));

        BigDecimal remainingBalance = orderRepository.findByGiftCode(card.code())
            .orElseThrow()
            .balance();
        return new RedemptionResult(amount, remainingBalance);
    }

    /** Codes are typed by people: ignore surrounding spaces and lowercase letters. */
    static String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }
}
