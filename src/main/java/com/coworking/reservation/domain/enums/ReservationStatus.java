package com.coworking.reservation.domain.enums;

public enum ReservationStatus {

    PENDING_PAYMENT((short) 1, "Pendiente de pago"),
    CONFIRMED((short) 2, "Confirmada"),
    CANCELLED((short) 3, "Cancelada");

    private final Short code;
    private final String label;

    ReservationStatus(Short code, String label) {
        this.code = code;
        this.label = label;
    }

    public Short getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static ReservationStatus fromCode(short code) {
        for (ReservationStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Estado de reserva no válido: " + code);
    }
}
