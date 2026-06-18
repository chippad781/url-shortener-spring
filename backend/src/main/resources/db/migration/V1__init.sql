-- LinkSnip schema (Spring Boot rebuild) — owned by Flyway, validated by Hibernate.

CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(254) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    display_name  VARCHAR(120),
    created_at    TIMESTAMPTZ  NOT NULL
);

CREATE UNIQUE INDEX idx_users_email ON users (email);

CREATE TABLE short_urls (
    id           BIGSERIAL PRIMARY KEY,
    short_code   VARCHAR(16)   NOT NULL,
    original_url VARCHAR(2048) NOT NULL,
    user_id      BIGINT        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    is_active    BOOLEAN       NOT NULL DEFAULT TRUE,
    expires_at   TIMESTAMPTZ,
    click_count  BIGINT        NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ   NOT NULL,
    updated_at   TIMESTAMPTZ   NOT NULL
);

-- unique B-tree on short_code: hit on every redirect, also the collision guard
CREATE UNIQUE INDEX idx_short_urls_short_code   ON short_urls (short_code);
-- covers the per-user list query (newest first)
CREATE INDEX        idx_short_urls_user_created  ON short_urls (user_id, created_at DESC);
-- covers active/expired filtering
CREATE INDEX        idx_short_urls_active_expires ON short_urls (is_active, expires_at);

CREATE TABLE click_events (
    id         BIGSERIAL PRIMARY KEY,
    url_id     BIGINT      NOT NULL REFERENCES short_urls (id) ON DELETE CASCADE,
    ip_address VARCHAR(45),
    user_agent VARCHAR(512),
    referer    VARCHAR(256),
    clicked_at TIMESTAMPTZ NOT NULL
);

-- covers analytics time-series queries (per-url, newest first)
CREATE INDEX idx_click_events_url_clicked ON click_events (url_id, clicked_at DESC);
