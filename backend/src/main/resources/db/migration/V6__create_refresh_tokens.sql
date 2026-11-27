-- Refresh tokens: long-lived, revocable, stored hashed (SHA-256).
-- Each login creates a "family"; each refresh rotates the token inside its family.
-- Reusing a revoked token revokes the whole family (token theft detection).
--
-- ON DELETE CASCADE: these are security data about a session, not accounting
-- records, so they are deleted with the account (GDPR erasure).
CREATE TABLE refresh_tokens (
    id          UUID         PRIMARY KEY,
    user_id     UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    family_id   UUID         NOT NULL,
    token_hash  VARCHAR(64)  NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ  NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL,
    revoked_at  TIMESTAMPTZ
);

CREATE INDEX refresh_tokens_user_id_idx ON refresh_tokens (user_id);
CREATE INDEX refresh_tokens_family_id_idx ON refresh_tokens (family_id);
