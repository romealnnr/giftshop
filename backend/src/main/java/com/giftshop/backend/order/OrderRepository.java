package com.giftshop.backend.order;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

/**
 * Orders are read either for their owner, or by their gift code.
 * There is deliberately no method that loads an order by id without its owner.
 */
@Repository
public class OrderRepository {

    private final OrderJpaRepository jpaRepository;

    public OrderRepository(OrderJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    /**
     * Saves the order only if its gift code is not already used.
     * Uniqueness is enforced by the database UNIQUE constraint.
     *
     * @return true if saved, false if the code already exists
     */
    public boolean saveIfCodeIsUnique(Order order) {
        try {
            jpaRepository.saveAndFlush(OrderEntity.from(order));
            return true;
        } catch (DataIntegrityViolationException e) {
            if (jpaRepository.existsByCode(order.code())) {
                return false;
            }
            throw e;
        }
    }

    public Optional<Order> findByIdForUser(UUID id, UUID userId) {
        return jpaRepository.findByIdAndUserId(id, userId)
            .map(OrderEntity::toOrder);
    }

    public List<Order> findAllForUser(UUID userId) {
        return jpaRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
            .map(OrderEntity::toOrder)
            .toList();
    }

    /**
     * Finds a gift card by its code. The code is a bearer secret: whoever knows it
     * may use the card, which is why no owner check is done here.
     */
    public Optional<Order> findByGiftCode(String code) {
        return jpaRepository.findByCode(code).map(OrderEntity::toOrder);
    }

    /**
     * Debits the balance only if it is high enough, in a single atomic statement.
     * Must be called inside a transaction.
     *
     * @return true if debited, false if the balance was too low
     */
    public boolean debitBalance(UUID orderId, BigDecimal amount) {
        return jpaRepository.debitBalance(orderId, amount) == 1;
    }
}
