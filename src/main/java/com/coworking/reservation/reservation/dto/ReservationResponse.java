package com.coworking.reservation.reservation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ReservationResponse(
        @Schema(description = "UUID de la reserva", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,
        ReservationUserResponse user,
        ReservationSpaceResponse space,
        ReservationStatusResponse status,
        @Schema(description = "Inicio de la reserva", example = "2027-01-10T10:00:00Z")
        OffsetDateTime startTime,
        @Schema(description = "Fin de la reserva", example = "2027-01-10T12:00:00Z")
        OffsetDateTime endTime,
        @Schema(description = "Precio total calculado", example = "50.00")
        BigDecimal totalPrice,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
