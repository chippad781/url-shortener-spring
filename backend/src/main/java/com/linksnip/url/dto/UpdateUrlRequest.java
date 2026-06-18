package com.linksnip.url.dto;

import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.time.Instant;

/** All fields optional — only non-null values are applied (PATCH-like PUT). */
public record UpdateUrlRequest(
        @URL @Size(max = 2048) String originalUrl,
        Boolean active,
        Instant expiresAt) {
}
