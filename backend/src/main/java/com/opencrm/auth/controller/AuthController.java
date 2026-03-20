package com.opencrm.auth.controller;

import com.opencrm.auth.model.AuthRequest;
import com.opencrm.auth.model.AuthResponse;
import com.opencrm.auth.model.User;
import com.opencrm.auth.service.AuthService;
import com.opencrm.common.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody AuthRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        AuthResponse response = authService.refresh(refreshToken);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> me(Authentication auth) {
        User user = authService.getCurrentUser(auth.getPrincipal().toString());
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "email", user.getEmail(),
                "fullName", user.getFullName() != null ? user.getFullName() : "",
                "active", user.getActive()
        )));
    }
}
