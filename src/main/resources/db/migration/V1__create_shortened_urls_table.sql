CREATE TABLE shortened_urls
(
    id               BIGSERIAL     PRIMARY KEY,
    hash             CHAR(4)       NOT NULL UNIQUE,
    original_url     VARCHAR(2048) NOT NULL,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    access_count     BIGINT        NOT NULL DEFAULT 0,
    last_accessed_at TIMESTAMPTZ
);

CREATE UNIQUE INDEX idx_hash
    ON shortened_urls (hash);

CREATE INDEX idx_original_url
    ON shortened_urls (original_url);