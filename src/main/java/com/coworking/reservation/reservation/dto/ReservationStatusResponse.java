package com.coworking.reservation.reservation.dto;

import com.coworking.reservation.domain.enums.ReservationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

public record ReservationStatusResponse(
        @Schema(description = "Código numérico del estado", example = "1")
        Short code,
        @Schema(description = "Etiqueta en español", example = "Pendiente de pago")
        String name
) {

    public static ReservationStatusResponse from(ReservationStatus status) {
        return new ReservationStatusResponse(status.getCode(), status.getLabel());
    }
}
