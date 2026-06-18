package com.linksnip.analytics;

import com.linksnip.url.ShortUrl;
import com.linksnip.url.UrlRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records a click off the redirect thread. Writing a ClickEvent (and bumping
 * the counter) must not add latency to the redirect itself, so this runs on
 * the dedicated "clickExecutor" pool. At higher scale this would be a durable
 * queue (Kafka/SQS) instead of an in-process executor.
 */
@Service
public class ClickRecorder {

    private final ClickEventRepository clickEventRepository;
    private final UrlRepository urlRepository;

    public ClickRecorder(ClickEventRepository clickEventRepository, UrlRepository urlRepository) {
        this.clickEventRepository = clickEventRepository;
        this.urlRepository = urlRepository;
    }

    @Async("clickExecutor")
    @Transactional
    public void record(Long urlId, String ip, String userAgent, String referer) {
        // getReferenceById avoids loading the full row just to set the FK
        ShortUrl ref = urlRepository.getReferenceById(urlId);
        clickEventRepository.save(new ClickEvent(ref, ip, userAgent, referer));
        urlRepository.incrementClickCount(urlId);   // atomic UPDATE, race-free
    }
}
