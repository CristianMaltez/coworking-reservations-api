package com.coworking.reservation.reservation.dto;

import com.coworking.reservation.domain.enums.ReservationStatus;

public record ReservationStatusResponse(
        Short code,
        String name
) {

    public static ReservationStatusResponse from(ReservationStatus status) {
        return new ReservationStatusResponse(status.getCode(), status.getLabel());
    }
}
