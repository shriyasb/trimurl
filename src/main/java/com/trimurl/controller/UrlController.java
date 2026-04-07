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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class UrlController {

    private final UrlService urlService;

    public UrlController(UrlService urlService) {
        this.urlService = urlService;
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/login";
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/login";
    }

    @GetMapping("/home")
    public String home() {
        return "home";
    }

    @PostMapping("/shorten")
    public String shortenUrl(@Valid @ModelAttribute UrlRequest request,
                             @AuthenticationPrincipal UserDetails user,
                             Model model) {
        String userId = user.getUsername();
        try {
            UrlResponse response = urlService.createShortUrl(request, userId);
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
    public String deleteUrl(@PathVariable String shortCode,
                           @AuthenticationPrincipal UserDetails user) {
        String userId = user.getUsername();
        urlService.deleteUrl(shortCode, userId);
        return "redirect:/dashboard";
    }

    @PostMapping("/toggle/{shortCode}")
    public String toggleUrl(@PathVariable String shortCode,
                           @AuthenticationPrincipal UserDetails user) {
        String userId = user.getUsername();
        urlService.toggleUrl(shortCode, userId);
        return "redirect:/dashboard?disabled=true";
    }

    @GetMapping("/analytics/{shortCode}")
    public String analytics(@PathVariable String shortCode,
                            @AuthenticationPrincipal UserDetails user,
                            Model model) {
        String userId = user.getUsername();
        UrlResponse url = urlService.getUrlAnalytics(shortCode, userId);
        if (url == null) {
            return "redirect:/dashboard";
        }
        Map<String, Long> stats = urlService.getClickStatsByHour(shortCode, userId);
        model.addAttribute("url", url);
        model.addAttribute("stats", stats);
        return "analytics";
    }

    @GetMapping("/test")
    public String test(@AuthenticationPrincipal UserDetails user, Model model) {
        String userId = user.getUsername();
        model.addAttribute("urls", urlService.getUserUrls(userId));
        return "test";
    }

    @GetMapping("/r/{shortCode}")
    public String redirect(@PathVariable String shortCode) {
        String originalUrl = urlService.redirectToOriginal(shortCode);
        if (originalUrl == null) {
            return "redirect:/error";
        }
        return "redirect:" + originalUrl;
    }

    @GetMapping("/error")
    public String error() {
        return "error";
    }

    // API Endpoints
    @PostMapping("/api/shorten")
    @ResponseBody
    public ResponseEntity<?> createShortUrl(@Valid @RequestBody UrlRequest request,
                                            @AuthenticationPrincipal UserDetails user) {
        try {
            String userId = user.getUsername();
            UrlResponse response = urlService.createShortUrl(request, userId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/api/urls")
    @ResponseBody
    public ResponseEntity<List<UrlResponse>> getUserUrls(@AuthenticationPrincipal UserDetails user) {
        String userId = user.getUsername();
        return ResponseEntity.ok(urlService.getUserUrls(userId));
    }
}