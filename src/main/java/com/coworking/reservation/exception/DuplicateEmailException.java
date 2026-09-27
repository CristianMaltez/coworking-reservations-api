package com.coworking.reservation.exception;

public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException() {
        super("El correo electrónico ya está registrado");
    }
}
