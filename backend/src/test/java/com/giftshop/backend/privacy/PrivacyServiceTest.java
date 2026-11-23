package com.giftshop.backend.privacy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.giftshop.backend.order.Order;
import com.giftshop.backend.order.OrderService;
import com.giftshop.backend.redemption.Redemption;
import com.giftshop.backend.redemption.RedemptionService;
import com.giftshop.backend.user.Role;
import com.giftshop.backend.user.User;
import com.giftshop.backend.user.UserService;

@ExtendWith(MockitoExtension.class)
class PrivacyServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private OrderService orderService;

    @Mock
    private RedemptionService redemptionService;

    @InjectMocks
    private PrivacyService privacyService;

    @Test
    void exportData_containsTheProfileOrdersAndRedemptions() {
        User alice = new User(UUID.randomUUID(), "alice@example.com", Role.USER, true, Instant.now());
        Order order = new Order(UUID.randomUUID(), alice.id(), 2L, "Carte Librairie",
            new BigDecimal("30.00"), "GIFT-AAAA-BBBB-CCCC", Instant.now());
        Redemption redemption = new Redemption(UUID.randomUUID(), UUID.randomUUID(), alice.id(),
            new BigDecimal("10.00"), Instant.now());
        when(userService.getById(alice.id())).thenReturn(alice);
        when(orderService.getAllForUser(alice.id())).thenReturn(List.of(order));
        when(redemptionService.getAllForUser(alice.id())).thenReturn(List.of(redemption));

        UserDataExport export = privacyService.exportData(alice.id());

        assertThat(export.profile()).isEqualTo(alice);
        assertThat(export.orders()).containsExactly(order);
        assertThat(export.redemptions()).containsExactly(redemption);
        assertThat(export.exportedAt()).isNotNull();
    }
}
