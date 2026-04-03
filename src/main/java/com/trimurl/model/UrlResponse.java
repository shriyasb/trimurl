package com.trimurl.model;

import java.time.Instant;

/**
 * Response object for shortened URL data.
 */
public class UrlResponse {
    private String shortCode;
    private String shortUrl;
    private String originalUrl;
    private Instant createdAt;
    private int totalClicks;

    public UrlResponse() {
    }

    public UrlResponse(String shortCode, String shortUrl, String originalUrl, Instant createdAt, int totalClicks) {
        this.shortCode = shortCode;
        this.shortUrl = shortUrl;
        this.originalUrl = originalUrl;
        this.createdAt = createdAt;
        this.totalClicks = totalClicks;
    }

    public String getShortCode() {
        return shortCode;
    }

    public void setShortCode(String shortCode) {
        this.shortCode = shortCode;
    }

    public String getShortUrl() {
        return shortUrl;
    }

    public void setShortUrl(String shortUrl) {
        this.shortUrl = shortUrl;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public int getTotalClicks() {
        return totalClicks;
    }

    public void setTotalClicks(int totalClicks) {
        this.totalClicks = totalClicks;
    }
}