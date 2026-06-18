package com.linksnip.analytics;

import com.linksnip.url.ShortUrl;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
        name = "click_events",
        indexes = {
                // covers analytics time-series queries (per-url, newest first)
                @Index(name = "idx_click_events_url_clicked", columnList = "url_id, clicked_at")
        }
)
public class ClickEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "url_id", nullable = false)
    private ShortUrl url;

    @Column(name = "ip_address", length = 45)   // fits IPv6
    private String ipAddress;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(length = 256)
    private String referer;

    @Column(name = "clicked_at", nullable = false, updatable = false)
    private Instant clickedAt;

    @PrePersist
    void onCreate() {
        this.clickedAt = Instant.now();
    }

    protected ClickEvent() {
        // JPA
    }

    public ClickEvent(ShortUrl url, String ipAddress, String userAgent, String referer) {
        this.url = url;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.referer = referer;
    }

    public Long getId() {
        return id;
    }

    public Instant getClickedAt() {
        return clickedAt;
    }
}
