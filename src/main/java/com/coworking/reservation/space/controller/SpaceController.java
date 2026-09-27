package com.coworking.reservation.space.controller;

import com.coworking.reservation.space.dto.CreateSpaceRequest;
import com.coworking.reservation.space.dto.SpaceResponse;
import com.coworking.reservation.space.dto.UpdateSpaceRequest;
import com.coworking.reservation.space.service.SpaceService;
import com.coworking.reservation.exception.ApiError;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/spaces")
@Tag(name = "Espacios", description = "Consulta y administración de espacios de coworking")
@SecurityRequirement(name = "bearerAuth")
public class SpaceController {

    private final SpaceService spaceService;

    public SpaceController(SpaceService spaceService) {
        this.spaceService = spaceService;
    }

    @GetMapping
    @Operation(summary = "Listar espacios", description = "Disponible para usuarios autenticados con rol USER o ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Espacios disponibles"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente, inválido, expirado o revocado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public List<SpaceResponse> findAll() {
        return spaceService.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar espacio", description = "Disponible para usuarios autenticados con rol USER o ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Espacio encontrado"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente, inválido, expirado o revocado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Espacio no encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public SpaceResponse findById(@Parameter(description = "Identificador UUID del espacio") @PathVariable UUID id) {
        return spaceService.findById(id);
    }

    @PostMapping
    @Operation(
            summary = "Crear espacio",
            description = "Requiere rol ADMIN. spaceTypeId debe existir en el catálogo de tipos de espacio; capacity debe ser positiva y hourlyRate es un valor monetario decimal."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Espacio creado"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente, inválido, expirado o revocado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Permisos insuficientes: se requiere ADMIN",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "422", description = "Error de validación o tipo de espacio inexistente",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<SpaceResponse> create(@Valid @RequestBody CreateSpaceRequest request) {
        SpaceResponse response = spaceService.create(request);
        return ResponseEntity.created(URI.create("/api/spaces/" + response.id())).body(response);
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Actualizar espacio",
            description = "Requiere rol ADMIN. spaceTypeId debe existir en el catálogo de tipos de espacio; capacity debe ser positiva y hourlyRate es un valor monetario decimal."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Espacio actualizado"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente, inválido, expirado o revocado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Permisos insuficientes: se requiere ADMIN",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Espacio no encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "422", description = "Error de validación o tipo de espacio inexistente",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public SpaceResponse update(@Parameter(description = "Identificador UUID del espacio") @PathVariable UUID id,
            @Valid @RequestBody UpdateSpaceRequest request) {
        return spaceService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar espacio", description = "Requiere rol ADMIN. No permite eliminar espacios que tienen reservas asociadas.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Espacio eliminado"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente, inválido, expirado o revocado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Permisos insuficientes: se requiere ADMIN",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Espacio no encontrado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "El espacio tiene reservas asociadas",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public void delete(@Parameter(description = "Identificador UUID del espacio") @PathVariable UUID id) {
        spaceService.delete(id);
    }
}
