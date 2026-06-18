package com.linksnip.analytics.dto;

import java.io.Serializable;

public record TopUrlPoint(
        Long urlId,
        String shortCode,
        String originalUrl,
        long clickCount) implements Serializable {
}
