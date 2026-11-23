package com.giftshop.backend.privacy;

import java.time.Instant;
import java.util.List;

import com.giftshop.backend.order.Order;
import com.giftshop.backend.redemption.Redemption;
import com.giftshop.backend.user.User;

/**
 * Everything the shop stores about a user (GDPR right of access, article 15),
 * in a machine-readable format (right to data portability, article 20).
 *
 * <p>The password hash is deliberately left out: it is a security secret,
 * useless to the user, and exporting it would only increase the risk of a leak.
 *
 * @param orders      the gift cards the user bought
 * @param redemptions the gift cards the user spent (possibly bought by someone else)
 */
public record UserDataExport(
    Instant exportedAt,
    User profile,
    List<Order> orders,
    List<Redemption> redemptions
) {
}
