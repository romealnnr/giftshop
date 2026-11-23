package com.giftshop.backend.redemption;

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
}
