package com.linksnip.url;

import com.linksnip.user.User;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
        name = "short_urls",
        indexes = {
                // unique B-tree on short_code — hit on every redirect
                @Index(name = "idx_short_urls_short_code", columnList = "short_code", unique = true),
                // covers the per-user list query (newest first)
                @Index(name = "idx_short_urls_user_created", columnList = "user_id, created_at"),
                // covers filtering active / expired URLs
                @Index(name = "idx_short_urls_active_expires", columnList = "is_active, expires_at")
        }
)
public class ShortUrl {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "short_code", nullable = false, unique = true, length = 16)
    private String shortCode;

    @Column(name = "original_url", nullable = false, length = 2048)
    private String originalUrl;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "click_count", nullable = false)
    private long clickCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    protected ShortUrl() {
        // JPA
    }

    public ShortUrl(String shortCode, String originalUrl, User user, Instant expiresAt) {
        this.shortCode = shortCode;
        this.originalUrl = originalUrl;
        this.user = user;
        this.expiresAt = expiresAt;
    }

    /** A URL is resolvable if it's active and not past its expiry. */
    public boolean isResolvable() {
        return active && (expiresAt == null || expiresAt.isAfter(Instant.now()));
    }

    public Long getId() {
        return id;
    }

    public String getShortCode() {
        return shortCode;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }

    public User getUser() {
        return user;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public long getClickCount() {
        return clickCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
