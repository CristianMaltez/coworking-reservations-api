package com.coworking.reservation.reservation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UpdateReservationStatusRequest(
        @NotNull(message = "El estado es obligatorio")
        @Positive(message = "El estado debe ser positivo")
        @Schema(description = "Código de estado: 1=Pendiente de pago, 2=Confirmada, 3=Cancelada", example = "3")
        Short statusCode
) {
}
