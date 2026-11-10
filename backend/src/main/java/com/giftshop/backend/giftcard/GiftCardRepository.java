package com.giftshop.backend.giftcard;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

@Repository 
public class GiftCardRepository {
    
    // simulate a db
    private final List<GiftCard> giftCards = List.of(
        new GiftCard(1L, "Carte Cinéma", "Loisirs", new BigDecimal("25.00")),
        new GiftCard(2L, "Carte Librairie", "Culture", new BigDecimal("30.00")),
        new GiftCard(3L, "Carte Restaurant", "Gastronomie", new BigDecimal("50.00")),
        new GiftCard(4L, "Carte Sport", "Sport", new BigDecimal("40.00"))
    );

    public List<GiftCard> findAll(){
        return giftCards;
    }

    public Optional<GiftCard> findById(Long id){
        return  giftCards.stream()
            .filter(card -> card.id().equals(id))
            .findFirst();
    }
}
