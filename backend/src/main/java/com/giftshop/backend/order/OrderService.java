package com.giftshop.backend.order;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.giftshop.backend.giftcard.GiftCard;
import com.giftshop.backend.giftcard.GiftCardService;

@Service
public class OrderService{

    static final int MAX_CODE_ATTEMPTS = 5;

    private final GiftCardService giftCardService;
    private final GiftCodeGenerator codeGenerator;
    private final OrderRepository orderRepository;

    public OrderService(GiftCardService giftCardService,
                        GiftCodeGenerator codeGenerator,
                        OrderRepository orderRepository) {
        this.giftCardService = giftCardService;
        this.codeGenerator = codeGenerator;
        this.orderRepository = orderRepository;
    }

    public Order create(Long giftCardId) {
        GiftCard giftCard = giftCardService.getById(giftCardId);

        for (int attempt = 1; attempt <= MAX_CODE_ATTEMPTS; attempt++) {
            Order order = new Order(
                UUID.randomUUID(),
                giftCard.id(),
                giftCard.name(),
                giftCard.amount(),
                codeGenerator.generate(),
                Instant.now()
            );
            if (orderRepository.saveIfCodeIsUnique(order)) {
                return order;
            }
        }

        throw new GiftCodeGenerationException(MAX_CODE_ATTEMPTS);
    }

    public Order getById(UUID id) {
        return orderRepository.findById(id)
            .orElseThrow(() -> new OrderNotFoundException(id));
    }
}