package com.giftshop.backend.order;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

/**
 * Orders can only be read for a given user: there is deliberately no method
 * that loads an order without checking who owns it.
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
}
