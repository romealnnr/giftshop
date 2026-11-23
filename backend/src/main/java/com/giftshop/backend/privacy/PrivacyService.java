package com.giftshop.backend.privacy;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.giftshop.backend.order.OrderService;
import com.giftshop.backend.redemption.RedemptionService;
import com.giftshop.backend.user.UserService;

/**
 * Single entry point for the GDPR rights of a user. When a new kind of personal
 * data is added to the shop, it must be added to the export and to the erasure here.
 *
 * <p>Personal data currently stored: the profile (users), the purchased gift cards
 * (orders.user_id) and the spent gift cards (redemptions.user_id). On erasure, the
 * user row is deleted and the database anonymizes orders and redemptions
 * (ON DELETE SET NULL), which are kept for accounting.
 */
@Service
public class PrivacyService {

    private final UserService userService;
    private final OrderService orderService;
    private final RedemptionService redemptionService;

    public PrivacyService(UserService userService,
                          OrderService orderService,
                          RedemptionService redemptionService) {
        this.userService = userService;
        this.orderService = orderService;
        this.redemptionService = redemptionService;
    }

    /** Collects all the data stored about the given user (right of access). */
    public UserDataExport exportData(UUID userId) {
        return new UserDataExport(
            Instant.now(),
            userService.getById(userId),
            orderService.getAllForUser(userId),
            redemptionService.getAllForUser(userId)
        );
    }

    /**
     * Deletes the account (right to erasure). Orders and redemptions are kept
     * for accounting, but anonymized by the database.
     */
    public void deleteAccount(UUID userId, String password) {
        userService.deleteAccount(userId, password);
    }
}
