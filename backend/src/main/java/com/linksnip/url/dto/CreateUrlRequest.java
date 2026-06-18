package com.linksnip.url.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.time.Instant;

public record CreateUrlRequest(
        @NotBlank @URL(message = "Must be a valid URL") @Size(max = 2048) String originalUrl,

        // optional vanity alias; null => server generates one
        @Pattern(regexp = "^[A-Za-z0-9]{3,16}$",
                 message = "Alias must be 3-16 alphanumeric characters")
        String customAlias,

        // optional expiry; null => never expires
        Instant expiresAt) {
}
