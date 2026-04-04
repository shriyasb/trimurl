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
    public ResponseEntity<?> createShortUrl(@Valid @RequestBody UrlRequest request, HttpServletRequest httpRequest) {
        try {
            String ipAddress = getClientIp(httpRequest);
            UrlResponse response = urlService.createShortUrl(request, ipAddress);
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
     * API: Get stats for a specific URL (simplified endpoint)
     */
    @GetMapping("/api/stats/{shortCode}")
    @ResponseBody
    public ResponseEntity<?> getStats(@PathVariable String shortCode) {
        UrlResponse analytics = urlService.getUrlAnalytics(shortCode);
        if (analytics == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "URL not found");
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(analytics);
    }

    /**
     * API: Check if short code exists
     */
    @GetMapping("/api/exists/{shortCode}")
    @ResponseBody
    public ResponseEntity<Map<String, Boolean>> checkExists(@PathVariable String shortCode) {
        Map<String, Boolean> response = new HashMap<>();
        response.put("exists", urlService.urlExists(shortCode));
        return ResponseEntity.ok(response);
    }

    /**
     * Web: Dashboard page with statistics
     */
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        Map<String, Object> stats = urlService.getDashboardStats();
        model.addAttribute("totalUrls", stats.get("totalUrls"));
        model.addAttribute("totalClicks", stats.get("totalClicks"));
        model.addAttribute("topUrls", stats.get("topUrls"));
        model.addAttribute("recentUrls", stats.get("recentUrls"));
        return "dashboard";
    }

    /**
     * Web: Stats page for specific URL
     */
    @GetMapping("/stats/{shortCode}")
    public String stats(@PathVariable String shortCode, Model model) {
        UrlResponse analytics = urlService.getUrlAnalytics(shortCode);
        if (analytics == null) {
            return "redirect:/error";
        }
        model.addAttribute("url", analytics);
        return "stats";
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
     * Redirect: Handle short URL access (MUST be last)
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