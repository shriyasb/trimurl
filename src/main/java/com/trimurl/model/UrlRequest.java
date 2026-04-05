package com.trimurl.model;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

/**
 * Request for creating shortened URLs.
 */
public class UrlRequest {

    @NotBlank(message = "URL is required")
    private String url;

    private String customShortCode;
    private Instant expiryDate;

    public UrlRequest() {
    }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getCustomShortCode() { return customShortCode; }
    public void setCustomShortCode(String customShortCode) { this.customShortCode = customShortCode; }

    public Instant getExpiryDate() { return expiryDate; }
    public void setExpiryDate(Instant expiryDate) { this.expiryDate = expiryDate; }
}