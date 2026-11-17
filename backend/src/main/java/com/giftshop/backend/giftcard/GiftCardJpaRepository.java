package com.giftshop.backend.giftcard;

import org.springframework.data.jpa.repository.JpaRepository;

interface GiftCardJpaRepository extends JpaRepository<GiftCardEntity, Long> {
}
