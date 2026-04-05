package com.trimurl.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * URL Document entity for MongoDB.
 */
@Document(collection = "urls")
public class UrlDocument {

    @Id
    private String id;
    private String shortCode;
    private String originalUrl;
    private Instant createdAt;
    private Instant expiryDate;
    private int totalClicks;
    private List<Instant> clickHistory = new ArrayList<>();
    private Instant lastAccessed;
    private String userId;

    public UrlDocument() {
    }

    public UrlDocument(String shortCode, String originalUrl, Instant createdAt) {
        this.shortCode = shortCode;
        this.originalUrl = originalUrl;
        this.createdAt = createdAt;
        this.totalClicks = 0;
    }

    public void addClick(Instant timestamp) {
        this.clickHistory.add(timestamp);
        this.totalClicks++;
        this.lastAccessed = Instant.now();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getShortCode() { return shortCode; }
    public void setShortCode(String shortCode) { this.shortCode = shortCode; }

    public String getOriginalUrl() { return originalUrl; }
    public void setOriginalUrl(String originalUrl) { this.originalUrl = originalUrl; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getExpiryDate() { return expiryDate; }
    public void setExpiryDate(Instant expiryDate) { this.expiryDate = expiryDate; }

    public int getTotalClicks() { return totalClicks; }
    public void setTotalClicks(int totalClicks) { this.totalClicks = totalClicks; }

    public List<Instant> getClickHistory() { return clickHistory; }
    public void setClickHistory(List<Instant> clickHistory) { this.clickHistory = clickHistory; }

    public Instant getLastAccessed() { return lastAccessed; }
    public void setLastAccessed(Instant lastAccessed) { this.lastAccessed = lastAccessed; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public boolean isExpired() {
        return expiryDate != null && Instant.now().isAfter(expiryDate);
    }
}