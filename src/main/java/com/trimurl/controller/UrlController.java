package com.trimurl.controller;

import com.trimurl.model.Click;
import com.trimurl.model.UrlRequest;
import com.trimurl.model.UrlResponse;
import com.trimurl.service.UrlService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for URL shortener operations.
 * Handles both API requests and web redirects.
 */
@Controller
public class UrlController {

    private final UrlService urlService;

    public UrlController(UrlService urlService) {
        this.urlService = urlService;
    }

    /**
     * API: Create a shortened URL
     */
    @PostMapping("/api/shorten")
    @ResponseBody
    public ResponseEntity<?> createShortUrl(@Valid @RequestBody UrlRequest request) {
        try {
            UrlResponse response = urlService.createShortUrl(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * API: Get all shortened URLs
     */
    @GetMapping("/api/urls")
    @ResponseBody
    public ResponseEntity<List<UrlResponse>> getAllUrls() {
        return ResponseEntity.ok(urlService.getAllUrls());
    }

    /**
     * API: Get analytics for a specific URL
     */
    @GetMapping("/api/analytics/{shortCode}")
    @ResponseBody
    public ResponseEntity<?> getAnalytics(@PathVariable String shortCode) {
        UrlResponse analytics = urlService.getUrlAnalytics(shortCode);
        if (analytics == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "URL not found");
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(analytics);
    }

    /**
     * API: Get click history for a specific URL
     */
    @GetMapping("/api/clicks/{shortCode}")
    @ResponseBody
    public ResponseEntity<?> getClicks(@PathVariable String shortCode) {
        List<Click> clicks = urlService.getClicks(shortCode);
        if (clicks == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "URL not found");
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(clicks);
    }

    /**
     * Redirect: Handle short URL access
     */
    @GetMapping("/{shortCode}")
    public String redirect(@PathVariable String shortCode, HttpServletRequest request) {
        String ipAddress = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        String originalUrl = urlService.redirectToOriginal(shortCode, ipAddress, userAgent);

        if (originalUrl == null) {
            return "redirect:/error";
        }

        return "redirect:" + originalUrl;
    }

    /**
     * Web: Dashboard page
     */
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        List<UrlResponse> urls = urlService.getAllUrls();
        model.addAttribute("urls", urls);
        return "dashboard";
    }

    /**
     * Web: Home page
     */
    @GetMapping("/")
    public String index() {
        return "index";
    }

    /**
     * Web: Error page
     */
    @GetMapping("/error")
    public String error() {
        return "error";
    }

    /**
     * Extract client IP address from request
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}