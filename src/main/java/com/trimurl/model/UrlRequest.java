package com.trimurl.model;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Request for creating shortened URLs.
 */
public class UrlRequest {

    @NotBlank(message = "URL is required")
    private String url;

    private String customShortCode;

    private String expiryDateStr;

    public UrlRequest() {
    }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getCustomShortCode() { return customShortCode; }
    public void setCustomShortCode(String customShortCode) { this.customShortCode = customShortCode; }

    public String getExpiryDateStr() { return expiryDateStr; }
    public void setExpiryDateStr(String expiryDateStr) { this.expiryDateStr = expiryDateStr; }

    public Instant getExpiryDate() {
        if (expiryDateStr == null || expiryDateStr.trim().isEmpty()) {
            return null;
        }
        try {
            LocalDateTime ldt = LocalDateTime.parse(expiryDateStr);
            return ldt.atZone(ZoneId.systemDefault()).toInstant();
        } catch (Exception e) {
            return null;
        }
    }
}