package com.linksnip.analytics.dto;

import java.io.Serializable;
import java.util.List;

/** Per-URL analytics summary. Serializable for Redis caching. */
public record AnalyticsResponse(
        Long urlId,
        String shortCode,
        long totalClicks,
        long clicksLast7Days,
        long clicksLast30Days,
        List<DailyClickPoint> dailySeries) implements Serializable {
}
