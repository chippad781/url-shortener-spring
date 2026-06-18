package com.linksnip.url;

import com.linksnip.common.PagedResponse;
import com.linksnip.ratelimit.RateLimitService;
import com.linksnip.security.AuthenticatedUser;
import com.linksnip.url.dto.CreateUrlRequest;
import com.linksnip.url.dto.UpdateUrlRequest;
import com.linksnip.url.dto.UrlResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/urls")
public class UrlController {

    private final UrlService urlService;
    private final RateLimitService rateLimitService;

    @Value("${app.ratelimit.url-create-per-hour}")
    private int createPerHour;

    public UrlController(UrlService urlService, RateLimitService rateLimitService) {
        this.urlService = urlService;
        this.rateLimitService = rateLimitService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UrlResponse create(@AuthenticationPrincipal AuthenticatedUser principal,
                              @Valid @RequestBody CreateUrlRequest req) {
        rateLimitService.check(
                "rl:url-create:" + principal.getId(),
                createPerHour,
                Duration.ofHours(1),
                "URL creation limit reached. Try again later.");
        return urlService.create(principal.getId(), req);
    }

    @GetMapping
    public PagedResponse<UrlResponse> list(@AuthenticationPrincipal AuthenticatedUser principal,
                                           @RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "20") int pageSize) {
        int safeSize = Math.min(Math.max(pageSize, 1), 100);
        return urlService.list(principal.getId(), page, safeSize);
    }

    @GetMapping("/{id}")
    public UrlResponse get(@AuthenticationPrincipal AuthenticatedUser principal,
                           @PathVariable Long id) {
        return urlService.get(principal.getId(), id);
    }

    @PutMapping("/{id}")
    public UrlResponse update(@AuthenticationPrincipal AuthenticatedUser principal,
                              @PathVariable Long id,
                              @Valid @RequestBody UpdateUrlRequest req) {
        return urlService.update(principal.getId(), id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthenticatedUser principal,
                       @PathVariable Long id) {
        urlService.delete(principal.getId(), id);
    }
}
