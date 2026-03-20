package com.opencrm.auth.model;

public record AuthResponse(
    String accessToken,
    String refreshToken,
    String username,
    String fullName
) {}
