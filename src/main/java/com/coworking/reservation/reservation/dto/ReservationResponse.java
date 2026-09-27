package com.coworking.reservation.reservation.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ReservationResponse(
        UUID id,
        ReservationUserResponse user,
        ReservationSpaceResponse space,
        ReservationStatusResponse status,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        BigDecimal totalPrice,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
