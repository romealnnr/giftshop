-- 1. Remaining value of each purchased gift card: a card can be spent in several times.
--    Added in three steps, because a NOT NULL column cannot be added to a table
--    that already contains rows without a value for it.
ALTER TABLE orders ADD COLUMN balance NUMERIC(10, 2);

-- Existing cards have never been used: their balance is their full amount.
UPDATE orders SET balance = amount;

ALTER TABLE orders
    ALTER COLUMN balance SET NOT NULL,
    ADD CONSTRAINT orders_balance_check CHECK (balance >= 0 AND balance <= amount);

-- 2. Every use of a gift card is recorded (ledger):
--    for each card, balance = amount - sum of its redemptions.
CREATE TABLE redemptions (
    id          UUID           PRIMARY KEY,
    order_id    UUID           NOT NULL REFERENCES orders (id),
    user_id     UUID           REFERENCES users (id) ON DELETE SET NULL,
    amount      NUMERIC(10, 2) NOT NULL CHECK (amount > 0),
    created_at  TIMESTAMPTZ    NOT NULL
);

CREATE INDEX redemptions_order_id_idx ON redemptions (order_id);
CREATE INDEX redemptions_user_id_idx ON redemptions (user_id);
