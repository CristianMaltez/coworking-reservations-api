package com.coworking.reservation.exception;

import java.util.UUID;

public class SpaceNotFoundException extends RuntimeException {

    public SpaceNotFoundException(UUID id) {
        super("Espacio no encontrado");
    }
}
