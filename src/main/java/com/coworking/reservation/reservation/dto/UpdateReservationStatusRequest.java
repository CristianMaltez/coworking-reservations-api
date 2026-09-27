package com.coworking.reservation.reservation.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UpdateReservationStatusRequest(
        @NotNull(message = "El estado es obligatorio")
        @Positive(message = "El estado debe ser positivo")
        Short statusCode
) {
}
