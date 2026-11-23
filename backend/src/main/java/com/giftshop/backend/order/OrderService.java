package com.giftshop.backend.order;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.giftshop.backend.giftcard.GiftCard;
import com.giftshop.backend.giftcard.GiftCardService;
import com.giftshop.backend.user.UserService;

@Service
public class OrderService {

    static final int MAX_CODE_ATTEMPTS = 5;

    private final GiftCardService giftCardService;
    private final GiftCodeGenerator codeGenerator;
    private final OrderRepository orderRepository;
    private final UserService userService;

    public OrderService(GiftCardService giftCardService,
                        GiftCodeGenerator codeGenerator,
                        OrderRepository orderRepository,
                        UserService userService) {
        this.giftCardService = giftCardService;
        this.codeGenerator = codeGenerator;
        this.orderRepository = orderRepository;
        this.userService = userService;
    }

    /**
     * Creates an order of the given gift card for the given user, and assigns it
     * a unique gift code.
     *
     * <p>The buyer's account is checked first: an access token stays valid for a
     * few minutes after the account is deleted or disabled.
     *
     * <p>The price and name are copied from the gift card at the time of purchase,
     * so later price changes do not affect existing orders. The amount always
     * comes from the server, never from the client.
     *
     * <p>If a generated code is already used, a new code is generated and the save
     * is retried, up to {@value #MAX_CODE_ATTEMPTS} attempts.
     *
     * <h2>Why this method is NOT {@code @Transactional}</h2>
     *
     * <p>Code uniqueness is enforced by the database {@code UNIQUE} constraint:
     * a duplicate code makes the {@code INSERT} fail. In PostgreSQL, once a
     * statement fails inside a transaction, the whole transaction is aborted and
     * every following statement is rejected ("current transaction is aborted").
     * Spring also marks the transaction as rollback-only as soon as an exception
     * goes through it.
     *
     * <p>With a single transaction around this method, the first collision would
     * therefore poison the transaction: the duplicate check and every retry would
     * fail, and the purchase would be lost even though a new code would have worked.
     *
     * <p>Without it, each repository call ({@code saveAndFlush}, {@code existsByCode})
     * runs in its own short transaction, so every retry starts from a fresh,
     * healthy transaction:
     *
     * <pre>
     * attempt 1: [tx A] INSERT duplicate code  -&gt; rejected, tx A rolled back
     * check:     [tx B] existsByCode           -&gt; true, so retry
     * attempt 2: [tx C] INSERT new code        -&gt; committed
     * </pre>
     *
     * <p>Nothing is lost: this method performs a single write (the order insert),
     * so there is nothing to group atomically. If several writes must become
     * atomic later (for example debiting a balance and creating the order), use
     * one transaction per attempt and keep the retry loop outside of it.
     *
     * @param giftCardId the id of the gift card to buy
     * @param userId the id of the buyer, taken from the access token
     * @return the created order, including its gift code
     * @throws com.giftshop.backend.user.InactiveAccountException if the buyer's account
     *         was deleted or disabled
     * @throws com.giftshop.backend.giftcard.GiftCardNotFoundException if no gift card exists with this id
     * @throws GiftCodeGenerationException if no unique code could be generated
     *         after {@value #MAX_CODE_ATTEMPTS} attempts
     */
    public Order create(Long giftCardId, UUID userId) {
        userService.requireActiveUser(userId);
        GiftCard giftCard = giftCardService.getById(giftCardId);

        for (int attempt = 1; attempt <= MAX_CODE_ATTEMPTS; attempt++) {
            Order order = new Order(
                UUID.randomUUID(),
                userId,
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

    /**
     * Returns an order of the given user.
     *
     * <p>An order that exists but belongs to another user is reported as not found
     * (404, not 403), so that nobody can find out whether an order id exists.
     *
     * @throws OrderNotFoundException if the order does not exist or is not the user's
     */
    public Order getById(UUID id, UUID userId) {
        return orderRepository.findByIdForUser(id, userId)
            .orElseThrow(() -> new OrderNotFoundException(id));
    }

    /** Returns the orders of the given user, newest first. */
    public List<Order> getAllForUser(UUID userId) {
        return orderRepository.findAllForUser(userId);
    }
}
