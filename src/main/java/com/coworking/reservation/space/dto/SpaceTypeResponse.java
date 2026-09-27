package com.coworking.reservation.space.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record SpaceTypeResponse(
        @Schema(description = "ID usado al crear o actualizar espacios", example = "2")
        Short id,
        @Schema(description = "Identificador técnico estable", example = "MEETING_ROOM")
        String code,
        @Schema(description = "Etiqueta en español", example = "Sala de reuniones")
        String name
) {
}
