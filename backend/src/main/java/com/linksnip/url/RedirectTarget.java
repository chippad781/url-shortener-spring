package com.linksnip.url;

import java.io.Serializable;
import java.time.Instant;

/**
 * Minimal projection cached on the redirect hot path so a redirect needs no
 * DB round-trip on a cache hit. Only active URLs are cached; expiry is
 * re-checked at read time because it's time-based and can't be event-evicted.
 */
public record RedirectTarget(Long id, String originalUrl, Instant expiresAt) implements Serializable {

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(Instant.now());
    }
}
