package com.coworking.reservation.exception;

public class AuthenticatedUserNotFoundException extends RuntimeException {

    public AuthenticatedUserNotFoundException() {
        super("El usuario autenticado no fue encontrado");
    }
}
