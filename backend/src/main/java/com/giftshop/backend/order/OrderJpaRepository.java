package com.giftshop.backend.order;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface OrderJpaRepository extends JpaRepository<OrderEntity, UUID> {

    boolean existsByCode(String code);

    Optional<OrderEntity> findByCode(String code);

    Optional<OrderEntity> findByIdAndUserId(UUID id, UUID userId);

    List<OrderEntity> findAllByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * Checks and debits the balance in a single atomic statement.
     *
     * @return the number of updated rows: 1 if debited, 0 if the balance was too low
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
        UPDATE OrderEntity o
        SET o.balance = o.balance - :amount
        WHERE o.id = :id AND o.balance >= :amount
        """)
    int debitBalance(@Param("id") UUID id, @Param("amount") BigDecimal amount);
}
