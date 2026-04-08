package com.trimurl.model;

import java.time.Instant;

public class UrlResponse {
    private String shortCode;
    private String shortUrl;
    private String originalUrl;
    private Instant createdAt;
    private Instant expiryDate;
    private int clickCount;
    private Instant lastAccessed;
    private String qrCode;
    private String userId;
    private boolean disabled;
    private Instant scheduledDisableAt;

    public UrlResponse() {}

    public UrlResponse(String shortCode, String shortUrl, String originalUrl, Instant createdAt, int totalClicks) {
        this.shortCode = shortCode; this.shortUrl = shortUrl; this.originalUrl = originalUrl;
        this.createdAt = createdAt; this.clickCount = totalClicks;
    }

    public String getShortCode() { return shortCode; }
    public void setShortCode(String shortCode) { this.shortCode = shortCode; }
    public String getShortUrl() { return shortUrl; }
    public void setShortUrl(String shortUrl) { this.shortUrl = shortUrl; }
    public String getOriginalUrl() { return originalUrl; }
    public void setOriginalUrl(String originalUrl) { this.originalUrl = originalUrl; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getExpiryDate() { return expiryDate; }
    public void setExpiryDate(Instant expiryDate) { this.expiryDate = expiryDate; }
    public int getClickCount() { return clickCount; }
    public void setClickCount(int clickCount) { this.clickCount = clickCount; }
    public Instant getLastAccessed() { return lastAccessed; }
    public void setLastAccessed(Instant lastAccessed) { this.lastAccessed = lastAccessed; }
    public String getQrCode() { return qrCode; }
    public void setQrCode(String qrCode) { this.qrCode = qrCode; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public boolean isDisabled() { return disabled; }
    public void setDisabled(boolean disabled) { this.disabled = disabled; }
    public Instant getScheduledDisableAt() { return scheduledDisableAt; }
    public void setScheduledDisableAt(Instant scheduledDisableAt) { this.scheduledDisableAt = scheduledDisableAt; }
}
