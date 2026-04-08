package com.trimurl.controller;

import com.trimurl.model.UrlDocument;
import com.trimurl.model.User;
import com.trimurl.repository.UrlRepository;
import com.trimurl.repository.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final UrlRepository urlRepository;

    public AdminController(UserRepository userRepository, UrlRepository urlRepository) {
        this.userRepository = userRepository;
        this.urlRepository = urlRepository;
    }

    @GetMapping({"", "/"})
    public String adminDashboard(Model model) {
        List<User> users = userRepository.findAll();
        long totalLinks  = urlRepository.count();
        long totalClicks = urlRepository.findAll().stream().mapToLong(UrlDocument::getTotalClicks).sum();
        long disabledUsers = users.stream().filter(User::isAccountDisabled).count();

        Map<String, Long> linkCounts  = users.stream().collect(Collectors.toMap(User::getId, u -> urlRepository.countByUserId(u.getId())));
        Map<String, Long> clickCounts = users.stream().collect(Collectors.toMap(User::getId,
            u -> urlRepository.findByUserId(u.getId()).stream().mapToLong(UrlDocument::getTotalClicks).sum()));

        model.addAttribute("users", users);
        model.addAttribute("totalLinks", totalLinks);
        model.addAttribute("totalClicks", totalClicks);
        model.addAttribute("totalUsers", users.size());
        model.addAttribute("disabledUsers", disabledUsers);
        model.addAttribute("userLinkCounts", linkCounts);
        model.addAttribute("userClickCounts", clickCounts);
        return "admin";
    }

    @GetMapping("/user/{userId}")
    public String userDetail(@PathVariable String userId, Model model) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return "redirect:/admin";
        model.addAttribute("targetUser", user);
        model.addAttribute("links", urlRepository.findAllByUserId(userId));
        return "admin-user";
    }

    @PostMapping("/disable-user/{userId}")
    public String disableUser(@PathVariable String userId) {
        userRepository.findById(userId).ifPresent(u -> { u.setAccountDisabled(true); userRepository.save(u); });
        return "redirect:/admin";
    }

    @PostMapping("/enable-user/{userId}")
    public String enableUser(@PathVariable String userId) {
        userRepository.findById(userId).ifPresent(u -> { u.setAccountDisabled(false); userRepository.save(u); });
        return "redirect:/admin";
    }

    @PostMapping("/delete-user/{userId}")
    public String deleteUser(@PathVariable String userId) {
        urlRepository.deleteAll(urlRepository.findAllByUserId(userId));
        userRepository.deleteById(userId);
        return "redirect:/admin";
    }

    @PostMapping("/disable-link/{shortCode}")
    public String disableLink(@PathVariable String shortCode, @RequestParam(defaultValue = "") String returnUserId) {
        urlRepository.findByShortCode(shortCode).ifPresent(doc -> { doc.setDisabled(true); urlRepository.save(doc); });
        return returnUserId.isEmpty() ? "redirect:/admin" : "redirect:/admin/user/" + returnUserId;
    }

    @PostMapping("/enable-link/{shortCode}")
    public String enableLink(@PathVariable String shortCode, @RequestParam(defaultValue = "") String returnUserId) {
        urlRepository.findByShortCode(shortCode).ifPresent(doc -> { doc.setDisabled(false); doc.setScheduledDisableAt(null); urlRepository.save(doc); });
        return returnUserId.isEmpty() ? "redirect:/admin" : "redirect:/admin/user/" + returnUserId;
    }

    @PostMapping("/delete-link/{shortCode}")
    public String deleteLink(@PathVariable String shortCode, @RequestParam(defaultValue = "") String returnUserId) {
        urlRepository.findByShortCode(shortCode).ifPresent(urlRepository::delete);
        return returnUserId.isEmpty() ? "redirect:/admin" : "redirect:/admin/user/" + returnUserId;
    }
}
