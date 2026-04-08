package com.trimurl.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.qrcode.QRCodeWriter;
import com.trimurl.model.UrlDocument;
import com.trimurl.model.UrlRequest;
import com.trimurl.model.UrlResponse;
import com.trimurl.repository.UrlRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

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
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash).substring(0, 8);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 not available", e);
        }
    }

    public UrlResponse createShortUrl(UrlRequest request, String userId) {
        String originalUrl = request.getUrl();
        if (!isValidUrl(originalUrl)) throw new IllegalArgumentException("Invalid URL format");

        String shortCode;
        if (request.getCustomShortCode() != null && !request.getCustomShortCode().isEmpty()) {
            shortCode = request.getCustomShortCode();
            if (urlRepository.existsByShortCode(shortCode)) throw new IllegalArgumentException("Custom short code already in use");
            if (!shortCode.matches("^[a-zA-Z0-9_-]{3,20}$")) throw new IllegalArgumentException("Invalid custom short code format");
        } else {
            shortCode = generateShortCode(originalUrl);
            int counter = 0;
            String finalCode = shortCode;
            while (urlRepository.existsByShortCode(finalCode)) { finalCode = shortCode + counter; counter++; }
            shortCode = finalCode;
        }

        UrlDocument doc = new UrlDocument(shortCode, originalUrl, Instant.now());
        doc.setExpiryDate(request.getExpiryDate());
        doc.setUserId(userId);
        doc.setQrCode(generateQRCode(baseUrl + "/r/" + shortCode));
        doc = urlRepository.save(doc);
        return toUrlResponse(doc);
    }

    private String generateQRCode(String content) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
            hints.put(EncodeHintType.MARGIN, 1);
            writer.encode(content, BarcodeFormat.QR_CODE, 200, 200, hints);
            return Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) { return null; }
    }

    public String redirectToOriginal(String shortCode) {
        return urlRepository.findByShortCode(shortCode).map(doc -> {
            if (doc.isDisabled() || doc.isExpired()) return null;
            doc.addClick(Instant.now());
            urlRepository.save(doc);
            return doc.getOriginalUrl();
        }).orElse(null);
    }

    public List<UrlResponse> getUserUrls(String userId) {
        return urlRepository.findByUserId(userId).stream().map(this::toUrlResponse).collect(Collectors.toList());
    }

    public List<UrlResponse> getTopUrls(String userId) {
        return urlRepository.findTop5ByUserIdOrderByTotalClicksDesc(userId).stream().map(this::toUrlResponse).collect(Collectors.toList());
    }

    public long getTotalUrls(String userId) { return urlRepository.countByUserId(userId); }

    public long getTotalClicks(String userId) {
        return urlRepository.findByUserId(userId).stream().mapToLong(UrlDocument::getTotalClicks).sum();
    }

    public UrlResponse getUrlAnalytics(String shortCode, String userId) {
        return urlRepository.findByShortCode(shortCode).filter(d -> userId.equals(d.getUserId())).map(this::toUrlResponse).orElse(null);
    }

    public Map<String, Long> getClickStatsByHour(String shortCode, String userId) {
        return urlRepository.findByShortCode(shortCode).filter(d -> userId.equals(d.getUserId())).map(doc -> {
            Map<String, Long> stats = new LinkedHashMap<>();
            for (Instant ts : doc.getClickHistory()) {
                String key = LocalDateTime.ofInstant(ts, ZoneId.systemDefault())
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:00"));
                stats.merge(key, 1L, Long::sum);
            }
            return stats;
        }).orElse(null);
    }

    public void deleteUrl(String shortCode, String userId) {
        urlRepository.findByShortCode(shortCode).filter(d -> userId.equals(d.getUserId())).ifPresent(urlRepository::delete);
    }

    public void toggleUrl(String shortCode, String userId) {
        urlRepository.findByShortCode(shortCode).filter(d -> userId.equals(d.getUserId())).ifPresent(doc -> {
            doc.setDisabled(!doc.isDisabled());
            urlRepository.save(doc);
        });
    }

    // Schedule disable in 1 hour
    public void scheduleDisable(String shortCode, String userId) {
        urlRepository.findByShortCode(shortCode).filter(d -> userId.equals(d.getUserId())).ifPresent(doc -> {
            doc.setScheduledDisableAt(Instant.now().plusSeconds(3600));
            urlRepository.save(doc);
        });
    }

    // Enable a disabled link
    public void enableUrl(String shortCode, String userId) {
        urlRepository.findByShortCode(shortCode).filter(d -> userId.equals(d.getUserId())).ifPresent(doc -> {
            doc.setDisabled(false);
            doc.setScheduledDisableAt(null);
            urlRepository.save(doc);
        });
    }

    // Auto-disable links every 60 seconds
    @Scheduled(fixedRate = 60000)
    public void processScheduledDisables() {
        urlRepository.findAll().stream().filter(UrlDocument::shouldBeDisabledNow).forEach(doc -> {
            doc.setDisabled(true);
            doc.setScheduledDisableAt(null);
            urlRepository.save(doc);
        });
    }

    // URL security check
    public Map<String, Object> checkUrlSecurity(String url) {
        Map<String, Object> result = new HashMap<>();
        try {
            URI uri = new URI(url);
            boolean isHttps = "https".equals(uri.getScheme());
            String host = uri.getHost() != null ? uri.getHost().toLowerCase() : "";

            List<String> suspiciousKeywords = List.of(
                "free-money", "login-verify", "verify-account", "account-confirm",
                "paypal-secure", "bank-login", "click-here-win", "prize-winner",
                "lucky-draw", "password-reset-now", "security-alert"
            );
            List<String> suspiciousTLDs = List.of(".tk", ".ml", ".ga", ".cf", ".gq");

            boolean hasKeyword = suspiciousKeywords.stream().anyMatch(url.toLowerCase()::contains);
            boolean hasBadTLD  = suspiciousTLDs.stream().anyMatch(host::endsWith);
            boolean isRawIP    = host.matches("\\d+\\.\\d+\\.\\d+\\.\\d+");

            boolean secure = isHttps && !hasKeyword && !hasBadTLD && !isRawIP;

            result.put("secure", secure);
            result.put("https", isHttps);
            result.put("host", host);

            if (!isHttps)        result.put("message", "⚠️ Insecure — HTTP links cannot be shortened. Use HTTPS.");
            else if (isRawIP)    result.put("message", "⚠️ Suspicious — raw IP address URLs cannot be shortened.");
            else if (hasBadTLD)  result.put("message", "⚠️ Suspicious domain extension detected. Cannot shorten.");
            else if (hasKeyword) result.put("message", "⚠️ Suspicious keywords detected in URL. Cannot shorten.");
            else                 result.put("message", "✅ Link is secure and can be shortened!");

        } catch (Exception e) {
            result.put("secure", false);
            result.put("message", "❌ Invalid URL format.");
        }
        return result;
    }

    private boolean isValidUrl(String url) {
        try {
            URI uri = new URI(url);
            return uri.getScheme() != null && (uri.getScheme().equals("http") || uri.getScheme().equals("https"));
        } catch (Exception e) { return false; }
    }

    private UrlResponse toUrlResponse(UrlDocument doc) {
        String shortUrl;
        try {
            shortUrl = ServletUriComponentsBuilder.fromCurrentContextPath().path("/r/").path(doc.getShortCode()).build().toUriString();
        } catch (Exception e) {
            shortUrl = baseUrl + "/r/" + doc.getShortCode();
        }
        UrlResponse r = new UrlResponse(doc.getShortCode(), shortUrl, doc.getOriginalUrl(), doc.getCreatedAt(), doc.getTotalClicks());
        r.setExpiryDate(doc.getExpiryDate());
        r.setLastAccessed(doc.getLastAccessed());
        r.setQrCode(doc.getQrCode());
        r.setUserId(doc.getUserId());
        r.setDisabled(doc.isDisabled());
        r.setScheduledDisableAt(doc.getScheduledDisableAt());
        return r;
    }
}
