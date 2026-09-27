package com.coworking.reservation.exception;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.Map;

@Schema(description = "Respuesta estándar de error de la API")
public record ApiError(
        @Schema(description = "Fecha y hora UTC del error", example = "2027-01-10T10:15:30Z")
        OffsetDateTime timestamp,
        @Schema(description = "Código HTTP", example = "422")
        int status,
        @Schema(description = "Razón HTTP en español", example = "Contenido no procesable")
        String error,
        @Schema(description = "Mensaje de error para el cliente", example = "Error de validación")
        String message,
        @Schema(description = "Errores por campo cuando aplica", example = "{\"email\":\"El correo electrónico debe tener un formato válido\"}")
        Map<String, String> fields
) {
}
