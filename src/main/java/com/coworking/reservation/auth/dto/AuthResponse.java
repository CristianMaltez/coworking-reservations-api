package com.coworking.reservation.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record AuthResponse(
        @Schema(description = "Token JWT de acceso", example = "eyJhbGciOiJIUzI1NiJ9...")
        String accessToken,
        @Schema(description = "Tipo de token", example = "Bearer")
        String tokenType,
        @Schema(description = "Vigencia del token en segundos", example = "900")
        long expiresIn
) {
}
