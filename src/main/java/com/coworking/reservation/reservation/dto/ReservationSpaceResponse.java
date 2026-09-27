package com.coworking.reservation.reservation.dto;

import java.util.UUID;

public record ReservationSpaceResponse(
        UUID id,
        String name
) {
}
