package com.coworking.reservation.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "El correo electrónico es obligatorio")
        @Email(message = "El correo electrónico debe tener un formato válido")
        @Size(max = 255, message = "El correo electrónico no debe superar 255 caracteres")
        @Schema(description = "Correo electrónico registrado", example = "usuario@example.com")
        String email,
        @NotBlank(message = "La contraseña es obligatoria")
        @Schema(description = "Contraseña del usuario", example = "Password123!")
        String password
) {
}
