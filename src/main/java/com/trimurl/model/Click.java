package com.trimurl.model;

import java.time.Instant;

/**
 * Click entity to track URL access history.
 */
public class Click {

    private String shortCode;
    private Instant timestamp;
    private String ipAddress;
    private String userAgent;

    public Click() {
    }

    public Click(Instant timestamp, String ipAddress, String userAgent) {
        this.timestamp = timestamp;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
    }

    public Click(String shortCode, Instant timestamp, String ipAddress, String userAgent) {
        this.shortCode = shortCode;
        this.timestamp = timestamp;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
    }

    public String getShortCode() {
        return shortCode;
    }

    public void setShortCode(String shortCode) {
        this.shortCode = shortCode;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
}