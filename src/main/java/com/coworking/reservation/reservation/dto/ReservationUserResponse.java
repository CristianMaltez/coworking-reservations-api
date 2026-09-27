package com.coworking.reservation.reservation.dto;

import java.util.UUID;

public record ReservationUserResponse(
        UUID id,
        String email,
        String firstName,
        String lastName
) {
}
