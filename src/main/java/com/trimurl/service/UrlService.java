package com.trimurl.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.qrcode.QRCodeWriter;
import com.trimurl.model.UrlDocument;
import com.trimurl.model.UrlRequest;
import com.trimurl.model.UrlResponse;
import com.trimurl.repository.UrlRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for URL shortener operations.
 */
@Service
public class UrlService {

    private final UrlRepository urlRepository;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    public UrlService(UrlRepository urlRepository) {
        this.urlRepository = urlRepository;
    }

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

    public UrlResponse createShortUrl(UrlRequest request, String userId) {
        String originalUrl = request.getUrl();

        if (!isValidUrl(originalUrl)) {
            throw new IllegalArgumentException("Invalid URL format");
        }

        String shortCode;
        if (request.getCustomShortCode() != null && !request.getCustomShortCode().isEmpty()) {
            shortCode = request.getCustomShortCode();
            if (urlRepository.existsByShortCode(shortCode)) {
                throw new IllegalArgumentException("Custom short code already in use");
            }
            if (!shortCode.matches("^[a-zA-Z0-9_-]{3,20}$")) {
                throw new IllegalArgumentException("Invalid custom short code format");
            }
        } else {
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
        document.setUserId(userId);

        // Generate QR Code
        String shortUrl = baseUrl + "/r/" + shortCode;
        String qrCode = generateQRCode(shortUrl);
        document.setQrCode(qrCode);

        document = urlRepository.save(document);
        return toUrlResponse(document);
    }

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
            return Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return null;
        }
    }

    public String redirectToOriginal(String shortCode) {
        Optional<UrlDocument> optDocument = urlRepository.findByShortCode(shortCode);

        if (optDocument.isEmpty()) {
            return null;
        }

        UrlDocument document = optDocument.get();

        if (document.isExpired()) {
            return null;
        }

        // Track click
        Instant now = Instant.now();
        document.addClick(now);
        urlRepository.save(document);

        return document.getOriginalUrl();
    }

    public List<UrlResponse> getUserUrls(String userId) {
        return urlRepository.findByUserId(userId).stream()
            .map(this::toUrlResponse)
            .collect(Collectors.toList());
    }

    public List<UrlResponse> getTopUrls(String userId) {
        return urlRepository.findTop5ByUserIdOrderByTotalClicksDesc(userId).stream()
            .map(this::toUrlResponse)
            .collect(Collectors.toList());
    }

    public long getTotalUrls(String userId) {
        return urlRepository.countByUserId(userId);
    }

    public long getTotalClicks(String userId) {
        return urlRepository.findByUserId(userId).stream()
            .mapToLong(UrlDocument::getTotalClicks)
            .sum();
    }

    public UrlResponse getUrlAnalytics(String shortCode, String userId) {
        Optional<UrlDocument> optDocument = urlRepository.findByShortCode(shortCode);
        if (optDocument.isEmpty()) {
            return null;
        }
        UrlDocument doc = optDocument.get();
        // Only allow owner to view analytics
        if (!doc.getUserId().equals(userId)) {
            return null;
        }
        return toUrlResponse(doc);
    }

    public Map<String, Long> getClickStatsByHour(String shortCode, String userId) {
        Optional<UrlDocument> optDocument = urlRepository.findByShortCode(shortCode);
        if (optDocument.isEmpty()) {
            return null;
        }
        UrlDocument doc = optDocument.get();
        if (!doc.getUserId().equals(userId)) {
            return null;
        }

        Map<String, Long> stats = new LinkedHashMap<>();
        List<Instant> history = doc.getClickHistory();

        for (Instant timestamp : history) {
            LocalDateTime ldt = LocalDateTime.ofInstant(timestamp, ZoneId.systemDefault());
            String hourKey = ldt.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:00"));
            stats.merge(hourKey, 1L, Long::sum);
        }

        return stats;
    }

    private boolean isValidUrl(String url) {
        try {
            java.net.URI uri = new java.net.URI(url);
            return uri.getScheme() != null && (uri.getScheme().equals("http") || uri.getScheme().equals("https"));
        } catch (Exception e) {
            return false;
        }
    }

    private UrlResponse toUrlResponse(UrlDocument document) {
        String shortUrl;
        try {
            shortUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/r/")
                .path(document.getShortCode())
                .build()
                .toUriString();
        } catch (Exception e) {
            shortUrl = baseUrl + "/r/" + document.getShortCode();
        }

        UrlResponse response = new UrlResponse(
            document.getShortCode(),
            shortUrl,
            document.getOriginalUrl(),
            document.getCreatedAt(),
            document.getTotalClicks()
        );
        response.setExpiryDate(document.getExpiryDate());
        response.setLastAccessed(document.getLastAccessed());
        response.setQrCode(document.getQrCode());
        response.setUserId(document.getUserId());
        return response;
    }
}