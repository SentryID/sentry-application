-- RN-F1-06.4: hashes dos refresh tokens já rotacionados; reapresentar um deles revoga a autorização inteira
CREATE TABLE oauth2_rotated_refresh_tokens (
    token_hash       VARCHAR(100) PRIMARY KEY,
    authorization_id VARCHAR(100) NOT NULL REFERENCES oauth2_authorization (id) ON DELETE CASCADE,
    rotated_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);
