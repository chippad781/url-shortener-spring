package com.linksnip.url;

import com.linksnip.analytics.ClickRecorder;
import com.linksnip.common.ClientIp;
import com.linksnip.ratelimit.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Duration;

/**
 * Public redirect endpoint: GET /{shortCode}. This is the latency-sensitive
 * hot path, so it: (1) serves from the Redis cache on a hit, (2) records the
 * click asynchronously, (3) issues a 302 to the original URL.
 */
@RestController
public class RedirectController {

    private final UrlService urlService;
    private final ClickRecorder clickRecorder;
    private final RateLimitService rateLimitService;

    @Value("${app.ratelimit.redirect-per-minute}")
    private int redirectPerMinute;

    public RedirectController(UrlService urlService,
                             ClickRecorder clickRecorder,
                             RateLimitService rateLimitService) {
        this.urlService = urlService;
        this.clickRecorder = clickRecorder;
        this.rateLimitService = rateLimitService;
    }

    @GetMapping("/{shortCode:[A-Za-z0-9]+}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode, HttpServletRequest request) {
        // protect the redirect endpoint from scraping/abuse (per-IP)
        rateLimitService.check(
                "rl:redirect:" + ClientIp.resolve(request),
                redirectPerMinute,
                Duration.ofMinutes(1),
                "Too many requests");

        RedirectTarget target = urlService.resolveForRedirect(shortCode);
        if (target == null || target.isExpired()) {
            if (target != null) {
                // expired-but-cached: drop it so we don't keep serving a hit
                urlService.evictRedirectCache(shortCode);
            }
            return ResponseEntity.notFound().build();
        }

        // fire-and-forget; never blocks the redirect
        clickRecorder.record(
                target.id(),
                ClientIp.resolve(request),
                request.getHeader("User-Agent"),
                request.getHeader("Referer"));

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(target.originalUrl()))
                .build();
    }
}
