package com.opencrm.auth.model;

import jakarta.validation.constraints.NotBlank;

public record AuthRequest(
    @NotBlank String username,
    @NotBlank String password
) {}
