package com.giftshop.backend.redemption;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface RedemptionJpaRepository extends JpaRepository<RedemptionEntity, UUID> {

    List<RedemptionEntity> findAllByUserIdOrderByCreatedAtDesc(UUID userId);
}
