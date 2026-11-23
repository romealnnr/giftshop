package com.giftshop.backend.privacy;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.giftshop.backend.order.OrderService;
import com.giftshop.backend.user.UserService;

@Service
public class PrivacyService {

    private final UserService userService;
    private final OrderService orderService;

    public PrivacyService(UserService userService, OrderService orderService) {
        this.userService = userService;
        this.orderService = orderService;
    }

    /** Collects all the data stored about the given user. */
    public UserDataExport exportData(UUID userId) {
        return new UserDataExport(
            Instant.now(),
            userService.getById(userId),
            orderService.getAllForUser(userId)
        );
    }
}
