package com.coworking.reservation.exception;

public class InvalidReservationTransitionException extends RuntimeException {

    public InvalidReservationTransitionException() {
        super("La transición de estado solicitada no es válida para la reserva actual");
    }
}
