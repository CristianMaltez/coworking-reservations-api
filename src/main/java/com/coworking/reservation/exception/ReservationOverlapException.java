package com.coworking.reservation.exception;

public class ReservationOverlapException extends RuntimeException {

    public ReservationOverlapException() {
        super("El espacio ya tiene una reserva que coincide con el horario solicitado");
    }
}
