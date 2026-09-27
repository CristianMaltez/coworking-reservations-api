package com.coworking.reservation.reservation.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ReservationSearchCriteria(
        UUID userId,
        UUID spaceId,
        Short statusCode,
        OffsetDateTime from,
        OffsetDateTime to,
        Integer page,
        Integer size
) {
}
