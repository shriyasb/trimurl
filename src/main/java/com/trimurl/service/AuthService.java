package com.trimurl.service;

import com.trimurl.model.User;
import com.trimurl.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User register(String email, String name, String password, String role) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already registered");
        }
        String safeRole = ("ADMIN".equalsIgnoreCase(role)) ? "ADMIN" : "USER";
        User user = new User(email, name, passwordEncoder.encode(password), safeRole);
        return userRepository.save(user);
    }

    public User register(String email, String name, String password) {
        return register(email, name, password, "USER");
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }
}
