package com.trimurl.model;

import jakarta.validation.constraints.NotBlank;

/**
 * Request object for creating a shortened URL.
 */
public class UrlRequest {
    @NotBlank(message = "URL is required")
    private String url;

    public UrlRequest() {
    }

    public UrlRequest(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}