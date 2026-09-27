package com.coworking.reservation.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "El correo electrónico es obligatorio")
        @Email(message = "El correo electrónico debe tener un formato válido")
        @Size(max = 255, message = "El correo electrónico no debe superar 255 caracteres")
        @Schema(description = "Correo electrónico único del usuario", example = "usuario@example.com")
        String email,
        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
        @Schema(description = "Contraseña del usuario. No se retorna en respuestas.", example = "Password123!")
        String password,
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no debe superar 100 caracteres")
        @Schema(description = "Nombre del usuario", example = "Ana")
        String firstName,
        @NotBlank(message = "El apellido es obligatorio")
        @Size(max = 100, message = "El apellido no debe superar 100 caracteres")
        @Schema(description = "Apellido del usuario", example = "García")
        String lastName
) {
}
