package com.giftshop.backend.order;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface OrderJpaRepository extends JpaRepository<OrderEntity, UUID> {

    boolean existsByCode(String code);

    Optional<OrderEntity> findByIdAndUserId(UUID id, UUID userId);

    List<OrderEntity> findAllByUserIdOrderByCreatedAtDesc(UUID userId);
}
