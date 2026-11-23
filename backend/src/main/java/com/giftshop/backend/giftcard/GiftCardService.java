package com.giftshop.backend.giftcard;

import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class GiftCardService {

    private final GiftCardRepository repository;

    public GiftCardService(GiftCardRepository repository) {
        this.repository = repository;
    }

    /** The catalog is read very often and changes rarely: it is cached in Redis. */
    @Cacheable("giftCards")
    public List<GiftCard> getAll() {
        return repository.findAll();
    }

    @Cacheable("giftCard")
    public GiftCard getById(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new GiftCardNotFoundException(id));
    }
}
