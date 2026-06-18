package com.linksnip.url.dto;

import com.linksnip.url.ShortUrl;

import java.io.Serializable;
import java.time.Instant;

/** Serializable so it can be stored in the Redis cache. */
public record UrlResponse(
        Long id,
        String shortCode,
        String shortUrl,
        String originalUrl,
        boolean active,
        Instant expiresAt,
        long clickCount,
        Instant createdAt,
        Instant updatedAt) implements Serializable {

    public static UrlResponse from(ShortUrl u, String baseUrl) {
        return new UrlResponse(
                u.getId(),
                u.getShortCode(),
                baseUrl + "/" + u.getShortCode(),
                u.getOriginalUrl(),
                u.isActive(),
                u.getExpiresAt(),
                u.getClickCount(),
                u.getCreatedAt(),
                u.getUpdatedAt());
    }
}
