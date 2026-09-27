package com.coworking.reservation.reservation.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CreateReservationRequest(
        @NotNull(message = "El espacio es obligatorio") UUID spaceId,
        @NotNull(message = "La fecha de inicio es obligatoria")
        @Future(message = "La fecha de inicio debe ser futura")
        OffsetDateTime startTime,
        @NotNull(message = "La fecha de finalización es obligatoria")
        @Future(message = "La fecha de finalización debe ser futura")
        OffsetDateTime endTime
) {
}
