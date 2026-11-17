package com.giftshop.backend.order;

import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

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

    public Optional<Order> findById(UUID id) {
        return jpaRepository.findById(id)
            .map(OrderEntity::toOrder);
    }
}