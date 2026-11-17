CREATE TABLE gift_cards (
    id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name      VARCHAR(100)   NOT NULL,
    category  VARCHAR(50)    NOT NULL,
    amount    NUMERIC(10, 2) NOT NULL CHECK (amount > 0)
);

CREATE TABLE orders (
    id              UUID           PRIMARY KEY,
    gift_card_id    BIGINT         NOT NULL REFERENCES gift_cards (id),
    gift_card_name  VARCHAR(100)   NOT NULL,
    amount          NUMERIC(10, 2) NOT NULL,
    code            VARCHAR(30)    NOT NULL UNIQUE,
    created_at      TIMESTAMPTZ    NOT NULL
);