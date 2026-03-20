package com.opencrm.auth.service;

import com.opencrm.auth.model.AuthRequest;
import com.opencrm.auth.model.AuthResponse;
import com.opencrm.auth.model.User;
import com.opencrm.auth.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse login(AuthRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid credentials");
        }

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new RuntimeException("Account is disabled");
        }

        String accessToken = jwtService.generateAccessToken(user.getId(), user.getUsername());
        String refreshToken = jwtService.generateRefreshToken(user.getId(), user.getUsername());

        return new AuthResponse(accessToken, refreshToken, user.getUsername(), user.getFullName());
    }

    public AuthResponse refresh(String refreshToken) {
        if (!jwtService.isValid(refreshToken)) {
            throw new RuntimeException("Invalid refresh token");
        }

        var userId = jwtService.getUserId(refreshToken);
        var username = jwtService.getUsername(refreshToken);

        String newAccessToken = jwtService.generateAccessToken(userId, username);
        return new AuthResponse(newAccessToken, refreshToken, username, null);
    }

    public User getCurrentUser(String userId) {
        return userRepository.findById(java.util.UUID.fromString(userId))
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
