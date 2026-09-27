package com.coworking.reservation.reservation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CreateReservationRequest(
        @Schema(description = "UUID del espacio a reservar", example = "550e8400-e29b-41d4-a716-446655440000")
        @NotNull(message = "El espacio es obligatorio") UUID spaceId,
        @NotNull(message = "La fecha de inicio es obligatoria")
        @Future(message = "La fecha de inicio debe ser futura")
        @Schema(description = "Fecha y hora futura de inicio", example = "2027-01-10T10:00:00Z")
        OffsetDateTime startTime,
        @NotNull(message = "La fecha de finalización es obligatoria")
        @Future(message = "La fecha de finalización debe ser futura")
        @Schema(description = "Fecha y hora futura de finalización. Debe ser posterior a startTime.", example = "2027-01-10T12:00:00Z")
        OffsetDateTime endTime
) {
}
