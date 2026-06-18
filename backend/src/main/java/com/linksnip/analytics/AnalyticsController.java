package com.linksnip.analytics;

import com.linksnip.analytics.dto.AnalyticsResponse;
import com.linksnip.analytics.dto.TopUrlPoint;
import com.linksnip.security.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/{urlId}")
    public AnalyticsResponse forUrl(@AuthenticationPrincipal AuthenticatedUser principal,
                                    @PathVariable Long urlId) {
        return analyticsService.forUrl(principal.getId(), urlId);
    }

    @GetMapping("/top-urls")
    public List<TopUrlPoint> topUrls(@AuthenticationPrincipal AuthenticatedUser principal,
                                     @RequestParam(defaultValue = "10") int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 50);
        return analyticsService.topUrls(principal.getId(), safeLimit);
    }
}
