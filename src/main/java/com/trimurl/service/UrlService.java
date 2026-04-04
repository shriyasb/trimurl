package com.trimurl.service;

import com.trimurl.model.Click;
import com.trimurl.model.UrlDocument;
import com.trimurl.model.UrlRequest;
import com.trimurl.model.UrlResponse;
import com.trimurl.repository.UrlRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service layer for URL shortener operations.
 * Handles URL creation, redirection, and analytics tracking.
 */
@Service
public class UrlService {

    private final UrlRepository urlRepository;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    public UrlService(UrlRepository urlRepository) {
        this.urlRepository = urlRepository;
    }

    /**
     * Short-code algorithm: Base62 encoding of MD5 hash
     *
     * Why Base62?
     * - Uses alphanumeric characters (0-9, a-z, A-Z) = 62 characters
     * - Provides good distribution of short codes
     * - URL-safe characters only
     *
     * Algorithm:
     * 1. Generate MD5 hash of the original URL
     * 2. Encode hash in Base64 and take first 8 characters
     * 3. This gives us an 8-character short code
     * 4. Check for collision and add suffix if needed
     *
     * Alternative algorithms considered:
     * - Sequential ID: Simple but reveals usage pattern
     * - Random alphanumeric: Good but requires collision check
     * - Base62 of timestamp: Too predictable
     */
    private String generateShortCode(String originalUrl) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(originalUrl.getBytes());
            String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
            return encoded.substring(0, 8);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not available", e);
        }
    }

    /**
     * Creates a shortened URL from the given original URL.
     */
    public UrlResponse createShortUrl(UrlRequest request) {
        String originalUrl = request.getUrl();

        // Validate URL format
        if (!isValidUrl(originalUrl)) {
            throw new IllegalArgumentException("Invalid URL format");
        }

        String shortCode = generateShortCode(originalUrl);

        // Handle potential collision by appending a counter
        int counter = 0;
        String finalShortCode = shortCode;
        while (urlRepository.findByShortCode(finalShortCode).isPresent()) {
            finalShortCode = shortCode + counter;
            counter++;
        }

        UrlDocument document = new UrlDocument(
            finalShortCode,
            originalUrl,
            Instant.now()
        );

        document = urlRepository.save(document);

        return toUrlResponse(document);
    }

    /**
     * Finds and redirects to the original URL, while tracking the click.
     */
    public String redirectToOriginal(String shortCode, String ipAddress, String userAgent) {
        Optional<UrlDocument> optDocument = urlRepository.findByShortCode(shortCode);

        if (optDocument.isEmpty()) {
            return null;
        }

        UrlDocument document = optDocument.get();

        // Track the click
        Click click = new Click(Instant.now(), ipAddress, extractBrowser(userAgent));
        document.addClick(click);
        urlRepository.save(document);

        return document.getOriginalUrl();
    }

    /**
     * Gets all shortened URLs for the dashboard.
     */
    public List<UrlResponse> getAllUrls() {
        return urlRepository.findAll().stream()
            .map(this::toUrlResponse)
            .collect(Collectors.toList());
    }

    /**
     * Gets analytics for a specific short code.
     */
    public UrlResponse getUrlAnalytics(String shortCode) {
        Optional<UrlDocument> optDocument = urlRepository.findByShortCode(shortCode);

        if (optDocument.isEmpty()) {
            return null;
        }

        return toUrlResponse(optDocument.get());
    }

    /**
     * Gets all clicks for a specific short code.
     */
    public List<Click> getClicks(String shortCode) {
        Optional<UrlDocument> optDocument = urlRepository.findByShortCode(shortCode);

        if (optDocument.isEmpty()) {
            return null;
        }

        return optDocument.get().getClicks();
    }

    private boolean isValidUrl(String url) {
        try {
            java.net.URI uri = new java.net.URI(url);
            return uri.getScheme() != null && (uri.getScheme().equals("http") || uri.getScheme().equals("https"));
        } catch (Exception e) {
            return false;
        }
    }

    private String extractBrowser(String userAgent) {
        if (userAgent == null || userAgent.isEmpty()) {
            return "Unknown";
        }

        if (userAgent.contains("Chrome")) {
            return "Chrome";
        } else if (userAgent.contains("Firefox")) {
            return "Firefox";
        } else if (userAgent.contains("Safari") && !userAgent.contains("Chrome")) {
            return "Safari";
        } else if (userAgent.contains("Edge")) {
            return "Edge";
        } else if (userAgent.contains("MSIE") || userAgent.contains("Trident")) {
            return "Internet Explorer";
        }

        return "Other";
    }

    private UrlResponse toUrlResponse(UrlDocument document) {
        String shortUrl;
        try {
            shortUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/")
                .path(document.getShortCode())
                .build()
                .toUriString();
        } catch (Exception e) {
            // Fallback for non-request contexts (tests, etc.)
            shortUrl = baseUrl + "/" + document.getShortCode();
        }
        return new UrlResponse(
            document.getShortCode(),
            shortUrl,
            document.getOriginalUrl(),
            document.getCreatedAt(),
            document.getClicks().size()
        );
    }
}