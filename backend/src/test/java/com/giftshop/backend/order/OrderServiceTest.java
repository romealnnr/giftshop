package com.giftshop.backend.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.giftshop.backend.giftcard.GiftCard;
import com.giftshop.backend.giftcard.GiftCardNotFoundException;
import com.giftshop.backend.giftcard.GiftCardService;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private GiftCardService giftCardService;

    @Mock
    private GiftCodeGenerator codeGenerator;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    private final GiftCard bookstore =
        new GiftCard(2L, "Carte Librairie", "Culture", new BigDecimal("30.00"));

    @Test
    void create_returnsOrderWithGiftCardDetailsAndCode() {
        when(giftCardService.getById(2L)).thenReturn(bookstore);
        when(codeGenerator.generate()).thenReturn("GIFT-AAAA-BBBB-CCCC");
        when(orderRepository.saveIfCodeIsUnique(any())).thenReturn(true);

        Order order = orderService.create(2L);

        assertThat(order.id()).isNotNull();
        assertThat(order.giftCardId()).isEqualTo(2L);
        assertThat(order.giftCardName()).isEqualTo("Carte Librairie");
        assertThat(order.amount()).isEqualByComparingTo("30.00");
        assertThat(order.code()).isEqualTo("GIFT-AAAA-BBBB-CCCC");
        assertThat(order.createdAt()).isNotNull();
    }

    @Test
    void create_retriesWithNewCode_whenCodeAlreadyExists() {
        when(giftCardService.getById(2L)).thenReturn(bookstore);
        when(codeGenerator.generate()).thenReturn("GIFT-AAAA-AAAA-AAAA", "GIFT-BBBB-BBBB-BBBB");
        when(orderRepository.saveIfCodeIsUnique(any())).thenReturn(false, true);

        Order order = orderService.create(2L);

        assertThat(order.code()).isEqualTo("GIFT-BBBB-BBBB-BBBB");
        verify(codeGenerator, times(2)).generate();
    }

    @Test
    void create_throwsException_afterTooManyCollisions() {
        when(giftCardService.getById(2L)).thenReturn(bookstore);
        when(codeGenerator.generate()).thenReturn("GIFT-AAAA-AAAA-AAAA");
        when(orderRepository.saveIfCodeIsUnique(any())).thenReturn(false);

        assertThatThrownBy(() -> orderService.create(2L))
            .isInstanceOf(GiftCodeGenerationException.class);

        verify(orderRepository, times(OrderService.MAX_CODE_ATTEMPTS)).saveIfCodeIsUnique(any());
    }

    @Test
    void create_doesNotSaveOrder_whenGiftCardDoesNotExist() {
        when(giftCardService.getById(99L)).thenThrow(new GiftCardNotFoundException(99L));

        assertThatThrownBy(() -> orderService.create(99L))
            .isInstanceOf(GiftCardNotFoundException.class);

        verify(orderRepository, never()).saveIfCodeIsUnique(any());
    }
}