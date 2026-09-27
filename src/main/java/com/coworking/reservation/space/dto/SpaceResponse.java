package com.coworking.reservation.space.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record SpaceResponse(
        UUID id,
        String name,
        Integer capacity,
        String location,
        BigDecimal hourlyRate,
        SpaceTypeResponse spaceType,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
