package com.coworking.reservation.reservation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record ReservationPageResponse(
        List<ReservationResponse> content,
        @Schema(description = "Número de página base cero", example = "0")
        int page,
        @Schema(description = "Tamaño de página aplicado", example = "20")
        int size,
        @Schema(description = "Total de reservas encontradas", example = "42")
        long totalElements,
        @Schema(description = "Total de páginas", example = "3")
        int totalPages
) {
}
