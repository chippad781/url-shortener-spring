package com.linksnip.analytics;

import com.linksnip.analytics.dto.AnalyticsResponse;
import com.linksnip.analytics.dto.DailyClickPoint;
import com.linksnip.analytics.dto.TopUrlPoint;
import com.linksnip.common.NotFoundException;
import com.linksnip.config.RedisConfig;
import com.linksnip.url.ShortUrl;
import com.linksnip.url.UrlRepository;
import com.linksnip.user.User;
import com.linksnip.user.UserRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class AnalyticsService {

    private final UrlRepository urlRepository;
    private final UserRepository userRepository;
    private final ClickEventRepository clickEventRepository;

    public AnalyticsService(UrlRepository urlRepository,
                            UserRepository userRepository,
                            ClickEventRepository clickEventRepository) {
        this.urlRepository = urlRepository;
        this.userRepository = userRepository;
        this.clickEventRepository = clickEventRepository;
    }

    /**
     * Per-URL analytics. Cached 5 minutes — these are eventually consistent and
     * the aggregations are comparatively expensive, so a short stale window is
     * an acceptable trade for skipping repeated GROUP BY scans.
     */
    @Cacheable(value = RedisConfig.CACHE_ANALYTICS, key = "#userId + ':' + #urlId")
    @Transactional(readOnly = true)
    public AnalyticsResponse forUrl(Long userId, Long urlId) {
        ShortUrl url = ownedOrThrow(userId, urlId);

        Instant now = Instant.now();
        Instant last7 = now.minus(7, ChronoUnit.DAYS);
        Instant last30 = now.minus(30, ChronoUnit.DAYS);

        long total = clickEventRepository.countByUrl(url);
        long c7 = clickEventRepository.countByUrlAndClickedAtAfter(url, last7);
        long c30 = clickEventRepository.countByUrlAndClickedAtAfter(url, last30);

        List<DailyClickPoint> series = clickEventRepository.countDailyClicks(url, last30).stream()
                .map(d -> new DailyClickPoint(d.getDay(), d.getTotal()))
                .toList();

        return new AnalyticsResponse(url.getId(), url.getShortCode(), total, c7, c30, series);
    }

    /**
     * Top URLs for the user by lifetime click count. Cached 10 minutes,
     * scoped per user.
     */
    @Cacheable(value = RedisConfig.CACHE_TOP_URLS, key = "#userId")
    @Transactional(readOnly = true)
    public List<TopUrlPoint> topUrls(Long userId, int limit) {
        User user = userRepository.getReferenceById(userId);
        return urlRepository.findByUserOrderByCreatedAtDesc(user, PageRequest.of(0, 1000))
                .stream()
                .sorted((a, b) -> Long.compare(b.getClickCount(), a.getClickCount()))
                .limit(limit)
                .map(u -> new TopUrlPoint(u.getId(), u.getShortCode(), u.getOriginalUrl(), u.getClickCount()))
                .toList();
    }

    private ShortUrl ownedOrThrow(Long userId, Long urlId) {
        User user = userRepository.getReferenceById(userId);
        return urlRepository.findByIdAndUser(urlId, user)
                .orElseThrow(() -> new NotFoundException("URL not found"));
    }
}
