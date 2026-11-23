package com.giftshop.backend.redemption;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

/** The ledger is append-only: redemptions are recorded, never updated nor deleted. */
@Repository
public class RedemptionRepository {

    private final RedemptionJpaRepository jpaRepository;

    public RedemptionRepository(RedemptionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    public void save(Redemption redemption) {
        jpaRepository.save(RedemptionEntity.from(redemption));
    }

    /** The redemptions made by the given user, newest first. */
    public List<Redemption> findAllForUser(UUID userId) {
        return jpaRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
            .map(RedemptionEntity::toRedemption)
            .toList();
    }
}
