package com.coworking.reservation.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "El correo electrónico es obligatorio")
        @Email(message = "El correo electrónico debe tener un formato válido")
        @Size(max = 255, message = "El correo electrónico no debe superar 255 caracteres")
        String email,
        @NotBlank(message = "La contraseña es obligatoria")
        String password
) {
}
