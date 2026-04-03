package com.trimurl.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * MongoDB document representing a shortened URL.
 * Contains the original URL, short code, creation timestamp, and click analytics.
 */
@Document(collection = "urls")
public class UrlDocument {
    @Id
    private String id;
    private String shortCode;
    private String originalUrl;
    private Instant createdAt;
    private List<Click> clicks;

    public UrlDocument() {
        this.clicks = new ArrayList<>();
    }

    public UrlDocument(String shortCode, String originalUrl, Instant createdAt) {
        this.shortCode = shortCode;
        this.originalUrl = originalUrl;
        this.createdAt = createdAt;
        this.clicks = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getShortCode() {
        return shortCode;
    }

    public void setShortCode(String shortCode) {
        this.shortCode = shortCode;
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

    public List<Click> getClicks() {
        return clicks;
    }

    public void setClicks(List<Click> clicks) {
        this.clicks = clicks;
    }

    public void addClick(Click click) {
        this.clicks.add(click);
    }
}