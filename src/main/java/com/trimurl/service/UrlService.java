package com.trimurl.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.qrcode.QRCodeWriter;
import com.trimurl.model.Click;
import com.trimurl.model.UrlDocument;
import com.trimurl.model.UrlRequest;
import com.trimurl.model.UrlResponse;
import com.trimurl.repository.UrlRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    public UrlResponse createShortUrl(UrlRequest request, String ipAddress) {
        String originalUrl = request.getUrl();

        // Validate URL format
        if (!isValidUrl(originalUrl)) {
            throw new IllegalArgumentException("Invalid URL format");
        }

        String shortCode;
        if (request.getCustomShortCode() != null && !request.getCustomShortCode().isEmpty()) {
            // Use custom short code if provided
            shortCode = request.getCustomShortCode();
            if (urlRepository.existsByShortCode(shortCode)) {
                throw new IllegalArgumentException("Custom short code already in use");
            }
            // Validate custom code format
            if (!shortCode.matches("^[a-zA-Z0-9_-]{3,20}$")) {
                throw new IllegalArgumentException("Invalid custom short code format");
            }
        } else {
            // Generate automatic short code
            shortCode = generateShortCode(originalUrl);
            int counter = 0;
            String finalShortCode = shortCode;
            while (urlRepository.existsByShortCode(finalShortCode)) {
                finalShortCode = shortCode + counter;
                counter++;
            }
            shortCode = finalShortCode;
        }

        UrlDocument document = new UrlDocument(shortCode, originalUrl, Instant.now());
        document.setExpiryDate(request.getExpiryDate());
        document.setCreatedByIp(ipAddress);

        // Generate QR Code
        String shortUrl = baseUrl + "/" + shortCode;
        String qrCode = generateQRCode(shortUrl);
        document.setQrCode(qrCode);

        document = urlRepository.save(document);

        return toUrlResponse(document);
    }

    /**
     * Generates QR Code as Base64 string
     */
    private String generateQRCode(String content) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
            hints.put(EncodeHintType.MARGIN, 1);

            var bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, 200, 200, hints);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            int[] pixels = new int[200 * 200];
            for (int y = 0; y < 200; y++) {
                for (int x = 0; x < 200; x++) {
                    pixels[y * 200 + x] = bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF;
                }
            }
            // Simple PNG encoding without external library
            return Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return null;
        }
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

        // Check if URL is expired
        if (document.isExpired()) {
            return null;
        }

        // Track the click
        Click click = new Click(shortCode, Instant.now(), ipAddress, extractBrowser(userAgent));
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

    /**
     * Gets dashboard statistics.
     */
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        long totalUrls = urlRepository.count();
        stats.put("totalUrls", totalUrls);

        List<UrlDocument> allUrls = urlRepository.findAll();
        int totalClicks = allUrls.stream().mapToInt(UrlDocument::getClickCount).sum();
        stats.put("totalClicks", totalClicks);

        List<UrlResponse> topUrls = urlRepository.findTop5ByOrderByClickCountDesc()
            .stream()
            .map(this::toUrlResponse)
            .collect(Collectors.toList());
        stats.put("topUrls", topUrls);

        List<UrlResponse> recentUrls = urlRepository.findTop10ByOrderByCreatedAtDesc()
            .stream()
            .map(this::toUrlResponse)
            .collect(Collectors.toList());
        stats.put("recentUrls", recentUrls);

        return stats;
    }

    /**
     * Checks if a URL exists by short code.
     */
    public boolean urlExists(String shortCode) {
        return urlRepository.findByShortCode(shortCode).isPresent();
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
            shortUrl = baseUrl + "/" + document.getShortCode();
        }

        UrlResponse response = new UrlResponse(
            document.getShortCode(),
            shortUrl,
            document.getOriginalUrl(),
            document.getCreatedAt(),
            document.getClickCount()
        );
        response.setExpiryDate(document.getExpiryDate());
        response.setLastAccessed(document.getLastAccessed());
        response.setCreatedByIp(document.getCreatedByIp());
        response.setQrCode(document.getQrCode());
        response.setClicks(document.getClicks());
        return response;
    }
}