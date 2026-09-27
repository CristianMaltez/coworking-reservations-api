package com.coworking.reservation.reservation.event;

import java.util.UUID;

public record ReservationConfirmedEvent(
        UUID reservationId,
        String userEmail
) {
}
