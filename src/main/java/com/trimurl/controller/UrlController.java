package com.trimurl.controller;

import com.trimurl.model.UrlRequest;
import com.trimurl.model.UrlResponse;
import com.trimurl.service.UrlService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
public class UrlController {

    private final UrlService urlService;

    public UrlController(UrlService urlService) { this.urlService = urlService; }

    @GetMapping("/")
    public String root() { return "redirect:/login"; }

    @GetMapping("/home")
    public String home(@AuthenticationPrincipal UserDetails user) {
        boolean isAdmin = user.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return isAdmin ? "redirect:/admin" : "home";
    }

    @PostMapping("/shorten")
    public String shortenUrl(@Valid @ModelAttribute UrlRequest request,
                             @AuthenticationPrincipal UserDetails user, Model model) {
        try {
            UrlResponse response = urlService.createShortUrl(request, user.getUsername());
            model.addAttribute("shortUrl", response.getShortUrl());
            model.addAttribute("originalUrl", request.getUrl());
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
        }
        return "home";
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails user, Model model) {
        String userId = user.getUsername();
        model.addAttribute("urls", urlService.getUserUrls(userId));
        model.addAttribute("totalUrls", urlService.getTotalUrls(userId));
        model.addAttribute("totalClicks", urlService.getTotalClicks(userId));
        model.addAttribute("topUrls", urlService.getTopUrls(userId));
        return "dashboard";
    }

    @PostMapping("/delete/{shortCode}")
    public String deleteUrl(@PathVariable String shortCode, @AuthenticationPrincipal UserDetails user) {
        urlService.deleteUrl(shortCode, user.getUsername());
        return "redirect:/dashboard";
    }

    // FIX 3: Immediately disable with redirect flag for instant toast notification
    @PostMapping("/disable/{shortCode}")
    public String disableUrl(@PathVariable String shortCode, @AuthenticationPrincipal UserDetails user) {
        urlService.disableNow(shortCode, user.getUsername());
        return "redirect:/dashboard?disabled=true";
    }

    // Schedule disable in 1 hour (kept for backward compat / expiry warning system)
    @PostMapping("/schedule-disable/{shortCode}")
    public String scheduleDisable(@PathVariable String shortCode, @AuthenticationPrincipal UserDetails user) {
        urlService.scheduleDisable(shortCode, user.getUsername());
        return "redirect:/dashboard?pendingDisable=true";
    }

    @PostMapping("/enable/{shortCode}")
    public String enableUrl(@PathVariable String shortCode, @AuthenticationPrincipal UserDetails user) {
        urlService.enableUrl(shortCode, user.getUsername());
        return "redirect:/dashboard?enabled=true";
    }

    // Legacy toggle now routes to immediate disable
    @PostMapping("/toggle/{shortCode}")
    public String toggleUrl(@PathVariable String shortCode, @AuthenticationPrincipal UserDetails user) {
        urlService.disableNow(shortCode, user.getUsername());
        return "redirect:/dashboard?disabled=true";
    }

    @GetMapping("/analytics/{shortCode}")
    public String analytics(@PathVariable String shortCode,
                            @AuthenticationPrincipal UserDetails user, Model model) {
        String userId = user.getUsername();
        UrlResponse url = urlService.getUrlAnalytics(shortCode, userId);
        if (url == null) return "redirect:/dashboard";
        model.addAttribute("url", url);
        model.addAttribute("stats", urlService.getClickStatsByHour(shortCode, userId));
        return "analytics";
    }

    @GetMapping("/test")
    public String test(@AuthenticationPrincipal UserDetails user, Model model) {
        model.addAttribute("urls", urlService.getUserUrls(user.getUsername()));
        return "test";
    }

    @GetMapping("/r/{shortCode}")
    public String redirect(@PathVariable String shortCode) {
        String url = urlService.redirectToOriginal(shortCode);
        return url != null ? "redirect:" + url : "redirect:/error";
    }

    @GetMapping("/error")
    public String error() { return "error"; }

    @PostMapping("/api/shorten")
    @ResponseBody
    public ResponseEntity<?> createShortUrl(@Valid @RequestBody UrlRequest request,
                                            @AuthenticationPrincipal UserDetails user) {
        try {
            return ResponseEntity.ok(urlService.createShortUrl(request, user.getUsername()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/api/urls")
    @ResponseBody
    public ResponseEntity<List<UrlResponse>> getUserUrls(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(urlService.getUserUrls(user.getUsername()));
    }

    @PostMapping("/api/check-url")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> checkUrl(@RequestBody Map<String, String> body) {
        String url = body.get("url");
        if (url == null || url.isBlank()) return ResponseEntity.badRequest().body(Map.of("secure", false, "message", "No URL provided"));
        return ResponseEntity.ok(urlService.checkUrlSecurity(url));
    }
}
