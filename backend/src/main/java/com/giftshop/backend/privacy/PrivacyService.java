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
 * (orders.user_id), the spent gift cards (redemptions.user_id) and the sessions
 * (refresh_tokens.user_id). On erasure, the user row is deleted: the database
 * anonymizes orders and redemptions (ON DELETE SET NULL), which are kept for
 * accounting, and deletes the sessions (ON DELETE CASCADE).
 *
 * <p>Sessions are not part of the export: they only contain security data
 * (a hash of a secret and technical dates) and are kept for 7 days at most.
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
