package com.coworking.reservation.exception;

import java.util.UUID;

public class SpaceHasReservationsException extends RuntimeException {

    public SpaceHasReservationsException(UUID id) {
        super("No se puede eliminar el espacio porque tiene reservas: " + id);
    }
}
