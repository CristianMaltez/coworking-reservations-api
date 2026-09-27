package com.coworking.reservation.exception;

import java.util.UUID;

public class ReservationNotFoundException extends RuntimeException {

    public ReservationNotFoundException(UUID id) {
        super("Reserva no encontrada: " + id);
    }
}
