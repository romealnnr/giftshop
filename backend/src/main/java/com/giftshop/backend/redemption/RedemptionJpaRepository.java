package com.giftshop.backend.redemption;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface RedemptionJpaRepository extends JpaRepository<RedemptionEntity, UUID> {
}
