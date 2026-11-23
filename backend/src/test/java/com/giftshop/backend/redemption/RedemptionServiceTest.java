package com.giftshop.backend.redemption;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.giftshop.backend.order.Order;
import com.giftshop.backend.order.OrderRepository;
import com.giftshop.backend.user.UserService;

@ExtendWith(MockitoExtension.class)
class RedemptionServiceTest {

    private static final String CODE = "GIFT-AAAA-BBBB-CCCC";

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private RedemptionRepository redemptionRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private RedemptionService redemptionService;

    private final UUID userId = UUID.randomUUID();

    private Order cardWithBalance(String balance) {
        return new Order(UUID.fromString("3f2a9c1e-7b4d-4e8a-9f12-6c5b8d0e4a71"), UUID.randomUUID(),
            3L, "Carte Restaurant", new BigDecimal("50.00"), new BigDecimal(balance), CODE, Instant.now());
    }

    @Test
    void redeem_debitsTheCard_andRecordsTheRedemption() {
        Order card = cardWithBalance("50.00");
        when(orderRepository.findByGiftCode(CODE))
            .thenReturn(Optional.of(card), Optional.of(cardWithBalance("30.00")));
        when(orderRepository.debitBalance(card.id(), new BigDecimal("20.00"))).thenReturn(true);

        RedemptionResult result = redemptionService.redeem(CODE, new BigDecimal("20.00"), userId);

        assertThat(result.redeemedAmount()).isEqualByComparingTo("20.00");
        assertThat(result.remainingBalance()).isEqualByComparingTo("30.00");
        verify(redemptionRepository).save(any(Redemption.class));
    }

    @Test
    void redeem_acceptsCodeTypedWithSpacesAndLowercase() {
        Order card = cardWithBalance("50.00");
        when(orderRepository.findByGiftCode(CODE)).thenReturn(Optional.of(card));
        when(orderRepository.debitBalance(card.id(), new BigDecimal("20.00"))).thenReturn(true);

        redemptionService.redeem("  gift-aaaa-bbbb-cccc ", new BigDecimal("20.00"), userId);

        verify(redemptionRepository).save(any(Redemption.class));
    }

    @Test
    void redeem_rejectsUnknownCode() {
        when(orderRepository.findByGiftCode(CODE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> redemptionService.redeem(CODE, new BigDecimal("20.00"), userId))
            .isInstanceOf(InvalidGiftCodeException.class);

        verify(orderRepository, never()).debitBalance(any(), any());
    }

    @Test
    void redeem_doesNotRecordAnything_whenBalanceIsTooLow() {
        Order card = cardWithBalance("10.00");
        when(orderRepository.findByGiftCode(CODE)).thenReturn(Optional.of(card));
        when(orderRepository.debitBalance(card.id(), new BigDecimal("20.00"))).thenReturn(false);

        assertThatThrownBy(() -> redemptionService.redeem(CODE, new BigDecimal("20.00"), userId))
            .isInstanceOf(InsufficientBalanceException.class);

        verify(redemptionRepository, never()).save(any());
    }

    @Test
    void getBalance_returnsTheCardBalance() {
        when(orderRepository.findByGiftCode(CODE)).thenReturn(Optional.of(cardWithBalance("30.00")));

        GiftCodeBalance balance = redemptionService.getBalance(CODE, userId);

        assertThat(balance.giftCardName()).isEqualTo("Carte Restaurant");
        assertThat(balance.amount()).isEqualByComparingTo("50.00");
        assertThat(balance.balance()).isEqualByComparingTo("30.00");
    }

    @Test
    void getBalance_rejectsUnknownCode() {
        when(orderRepository.findByGiftCode(CODE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> redemptionService.getBalance(CODE, userId))
            .isInstanceOf(InvalidGiftCodeException.class);
    }
}
