package com.coworking.reservation.catalog.controller;

import com.coworking.reservation.domain.enums.ReservationStatus;
import com.coworking.reservation.repository.SpaceTypeRepository;
import com.coworking.reservation.reservation.dto.ReservationStatusResponse;
import com.coworking.reservation.space.dto.SpaceTypeResponse;
import com.coworking.reservation.exception.ApiError;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/catalogs")
@Tag(name = "Catálogos", description = "Catálogos de referencia usados por la API")
@SecurityRequirement(name = "bearerAuth")
public class CatalogController {

    private final SpaceTypeRepository spaceTypeRepository;

    public CatalogController(SpaceTypeRepository spaceTypeRepository) {
        this.spaceTypeRepository = spaceTypeRepository;
    }

    @GetMapping("/reservation-statuses")
    @Operation(summary = "Listar estados de reserva", description = "Devuelve los códigos numéricos usados para filtrar o actualizar reservas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estados de reserva",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ReservationStatusResponse.class)),
                            examples = @ExampleObject(value = """
                                    [
                                      {"code":1,"name":"Pendiente de pago"},
                                      {"code":2,"name":"Confirmada"},
                                      {"code":3,"name":"Cancelada"}
                                    ]
                                    """))),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente, inválido, expirado o revocado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public List<ReservationStatusResponse> reservationStatuses() {
        return Arrays.stream(ReservationStatus.values())
                .map(ReservationStatusResponse::from)
                .toList();
    }

    @GetMapping("/space-types")
    @Operation(
            summary = "Listar tipos de espacio",
            description = "id se usa al crear o actualizar espacios; code es un identificador técnico estable; name es la etiqueta en español."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tipos de espacio",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = SpaceTypeResponse.class)),
                            examples = @ExampleObject(value = """
                                    [
                                      {"id":1,"code":"DESK","name":"Escritorio"},
                                      {"id":2,"code":"MEETING_ROOM","name":"Sala de reuniones"},
                                      {"id":3,"code":"PRIVATE_OFFICE","name":"Oficina privada"}
                                    ]
                                    """))),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente, inválido, expirado o revocado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public List<SpaceTypeResponse> spaceTypes() {
        return spaceTypeRepository.findAllByOrderByIdAsc().stream()
                .map(spaceType -> new SpaceTypeResponse(spaceType.getId(), spaceType.getCode(), spaceType.getName()))
                .toList();
    }
}
