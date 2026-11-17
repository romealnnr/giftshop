package com.giftshop.backend.order;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface OrderJpaRepository extends JpaRepository<OrderEntity, UUID> {

    boolean existsByCode(String code);
}
