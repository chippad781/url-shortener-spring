package com.linksnip.url;

import com.linksnip.common.ConflictException;
import com.linksnip.common.NotFoundException;
import com.linksnip.common.PagedResponse;
import com.linksnip.config.RedisConfig;
import com.linksnip.url.dto.CreateUrlRequest;
import com.linksnip.url.dto.UpdateUrlRequest;
import com.linksnip.url.dto.UrlResponse;
import com.linksnip.user.User;
import com.linksnip.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * All URL business logic. Controllers stay thin: they parse HTTP and delegate
 * here. Cache invalidation lives next to the writes so it's impossible to
 * forget when a new caller is added.
 *
 * Note on eviction: we evict via CacheManager rather than @CacheEvict because
 * update()/delete() would be calling the evict method on `this` — a Spring
 * self-invocation that bypasses the caching proxy and silently does nothing.
 * Going through CacheManager directly is proxy-independent and explicit.
 */
@Service
public class UrlService {

    private final UrlRepository urlRepository;
    private final UserRepository userRepository;
    private final ShortCodeGenerator shortCodeGenerator;
    private final CacheManager cacheManager;
    private final String baseUrl;

    public UrlService(UrlRepository urlRepository,
                      UserRepository userRepository,
                      ShortCodeGenerator shortCodeGenerator,
                      CacheManager cacheManager,
                      @Value("${app.base-url}") String baseUrl) {
        this.urlRepository = urlRepository;
        this.userRepository = userRepository;
        this.shortCodeGenerator = shortCodeGenerator;
        this.cacheManager = cacheManager;
        this.baseUrl = baseUrl;
    }

    // ----- redirect hot path -------------------------------------------------

    /**
     * Cached lookup for redirects. Only active URLs are cached. Returns null
     * (not cached) when missing/inactive; the caller validates expiry.
     * Called from RedirectController (a different bean), so the @Cacheable
     * proxy is honoured.
     */
    @Cacheable(value = RedisConfig.CACHE_URL, key = "#shortCode", unless = "#result == null")
    @Transactional(readOnly = true)
    public RedirectTarget resolveForRedirect(String shortCode) {
        return urlRepository.findByShortCode(shortCode)
                .filter(ShortUrl::isActive)
                .map(u -> new RedirectTarget(u.getId(), u.getOriginalUrl(), u.getExpiresAt()))
                .orElse(null);
    }

    public void evictRedirectCache(String shortCode) {
        Cache cache = cacheManager.getCache(RedisConfig.CACHE_URL);
        if (cache != null) {
            cache.evictIfPresent(shortCode);
        }
    }

    // ----- CRUD --------------------------------------------------------------

    @Transactional
    public UrlResponse create(Long userId, CreateUrlRequest req) {
        User user = userRepository.getReferenceById(userId);

        String code;
        if (req.customAlias() != null && !req.customAlias().isBlank()) {
            code = req.customAlias();
            if (urlRepository.existsByShortCode(code)) {
                throw new ConflictException("That alias is already taken");
            }
        } else {
            code = shortCodeGenerator.generateUnique();
        }

        ShortUrl url = new ShortUrl(code, req.originalUrl(), user, req.expiresAt());
        return UrlResponse.from(urlRepository.save(url), baseUrl);
    }

    @Transactional(readOnly = true)
    public PagedResponse<UrlResponse> list(Long userId, int page, int size) {
        User user = userRepository.getReferenceById(userId);
        Page<ShortUrl> result = urlRepository.findByUserOrderByCreatedAtDesc(
                user, PageRequest.of(Math.max(page - 1, 0), size));
        return PagedResponse.from(result, u -> UrlResponse.from(u, baseUrl));
    }

    @Transactional(readOnly = true)
    public UrlResponse get(Long userId, Long id) {
        return UrlResponse.from(ownedOrThrow(userId, id), baseUrl);
    }

    @Transactional
    public UrlResponse update(Long userId, Long id, UpdateUrlRequest req) {
        ShortUrl url = ownedOrThrow(userId, id);
        if (req.originalUrl() != null) {
            url.setOriginalUrl(req.originalUrl());
        }
        if (req.active() != null) {
            url.setActive(req.active());
        }
        if (req.expiresAt() != null) {
            url.setExpiresAt(req.expiresAt());
        }
        ShortUrl saved = urlRepository.save(url);
        evictRedirectCache(saved.getShortCode());   // keep the redirect cache honest
        return UrlResponse.from(saved, baseUrl);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        ShortUrl url = ownedOrThrow(userId, id);
        String code = url.getShortCode();
        urlRepository.delete(url);
        evictRedirectCache(code);
    }

    private ShortUrl ownedOrThrow(Long userId, Long id) {
        User user = userRepository.getReferenceById(userId);
        return urlRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new NotFoundException("URL not found"));
    }
}
