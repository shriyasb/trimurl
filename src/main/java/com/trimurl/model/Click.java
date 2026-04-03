package com.trimurl.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

/**
 * Represents a single click event on a shortened URL.
 * Stores analytics data including timestamp, IP address, and browser info.
 */
public class Click {
    @Id
    private String id;
    private Instant timestamp;
    private String ipAddress;
    private String browser;

    public Click() {
    }

    public Click(Instant timestamp, String ipAddress, String browser) {
        this.timestamp = timestamp;
        this.ipAddress = ipAddress;
        this.browser = browser;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getBrowser() {
        return browser;
    }

    public void setBrowser(String browser) {
        this.browser = browser;
    }
}