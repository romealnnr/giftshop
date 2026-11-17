package com.giftshop.backend.giftcard;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class GiftCardRepository {

    private final GiftCardJpaRepository jpaRepository;

    public GiftCardRepository(GiftCardJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    public List<GiftCard> findAll() {
        return jpaRepository.findAll(Sort.by("id")).stream()
            .map(GiftCardEntity::toGiftCard)
            .toList();
    }

    public Optional<GiftCard> findById(Long id) {
        return jpaRepository.findById(id)
            .map(GiftCardEntity::toGiftCard);
    }
}