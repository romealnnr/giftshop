-- Link each order to its buyer.
--
-- Nullable on purpose:
-- * orders created before user accounts existed have no buyer;
-- * when a user exercises the right to erasure (GDPR), the orders are kept
--   for accounting but anonymized: user_id is set to NULL.
ALTER TABLE orders
    ADD COLUMN user_id UUID REFERENCES users (id) ON DELETE SET NULL;

-- Orders are always searched by buyer: an index keeps that fast.
CREATE INDEX orders_user_id_idx ON orders (user_id);
