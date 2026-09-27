package com.coworking.reservation.reservation.controller;

import com.coworking.reservation.reservation.dto.CreateReservationRequest;
import com.coworking.reservation.reservation.dto.ReservationPageResponse;
import com.coworking.reservation.reservation.dto.ReservationResponse;
import com.coworking.reservation.reservation.dto.ReservationSearchCriteria;
import com.coworking.reservation.reservation.dto.UpdateReservationStatusRequest;
import com.coworking.reservation.reservation.service.ReservationService;
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

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/reservations")
@Tag(name = "Reservas", description = "Creación, consulta y actualización de estado de reservas")
@SecurityRequirement(name = "bearerAuth")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    @Operation(
            summary = "Crear reserva",
            description = "Crea una reserva para el usuario autenticado. Las fechas deben ser futuras, startTime debe ser anterior a endTime, la reserva inicia como Pendiente de pago y se rechazan traslapes con reservas activas."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Reserva creada"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente, inválido, expirado o revocado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Existe una reserva activa traslapada para el espacio solicitado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "422", description = "Error de validación, intervalo inválido o espacio inexistente",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<ReservationResponse> create(@Valid @RequestBody CreateReservationRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        ReservationResponse response = reservationService.create(request, jwt);
        return ResponseEntity.created(URI.create("/api/reservations/" + response.id())).body(response);
    }

    @GetMapping
    @Operation(
            summary = "Buscar reservas",
            description = "Devuelve reservas que intersectan el intervalo solicitado. page es BASE CERO: page=0 es la primera página y page=1 es la segunda. Tamaño por defecto: 20; máximo: 100. USER solo ve sus propias reservas; ADMIN puede consultar todas. Un USER que filtra otro usuario recibe 403. Códigos de estado: 1=Pendiente de pago, 2=Confirmada, 3=Cancelada."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de reservas"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente, inválido, expirado o revocado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "El usuario autenticado no puede consultar reservas de otro usuario",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "422", description = "Filtros semánticamente inválidos",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ReservationPageResponse findAll(
            @Parameter(description = "UUID del usuario. ADMIN puede filtrar cualquier usuario; USER solo el propio.")
            @RequestParam(required = false) UUID userId,
            @Parameter(description = "UUID del espacio") @RequestParam(required = false) UUID spaceId,
            @Parameter(description = "Código de estado: 1=Pendiente de pago, 2=Confirmada, 3=Cancelada", example = "1")
            @RequestParam(required = false) Short statusCode,
            @Parameter(description = "Inicio del intervalo de búsqueda. Retorna reservas que intersectan el intervalo.", example = "2027-01-10T10:00:00Z")
            @RequestParam(required = false) OffsetDateTime from,
            @Parameter(description = "Fin del intervalo de búsqueda. Debe ser posterior a from cuando ambos se envían.", example = "2027-01-10T12:00:00Z")
            @RequestParam(required = false) OffsetDateTime to,
            @Parameter(description = "Número de página base cero. page=0 es la primera página; page=1 es la segunda.", example = "0")
            @RequestParam(required = false) Integer page,
            @Parameter(description = "Tamaño de página. Por defecto 20; máximo 100.", example = "20")
            @RequestParam(required = false) Integer size,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return reservationService.findAll(new ReservationSearchCriteria(userId, spaceId, statusCode, from, to, page, size), jwt);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar reserva", description = "Disponible para el propietario de la reserva o ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reserva encontrada"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente, inválido, expirado o revocado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "USER intenta acceder a una reserva de otro usuario",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Reserva no encontrada",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ReservationResponse findById(@Parameter(description = "Identificador UUID de la reserva") @PathVariable UUID id,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return reservationService.findById(id, jwt);
    }

    @PatchMapping("/{id}")
    @Operation(
            summary = "Actualizar estado de reserva",
            description = "Actualiza el estado. Para statusCode=2 (Confirmada), ADMIN debe enviar paymentMethod y la API valida el pago externo antes de confirmar. Si el servicio de pago no está disponible, la reserva permanece Pendiente de pago y se retorna sin fallar la solicitud. Para statusCode=3 (Cancelada), paymentMethod no es requerido. USER solo puede cancelar su propia reserva. Códigos: 1=Pendiente de pago, 2=Confirmada, 3=Cancelada."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado actualizado"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente, inválido, expirado o revocado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Permisos insuficientes para actualizar esta reserva o estado",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Reserva no encontrada",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Transición de estado inválida",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "422", description = "Código de estado inválido, paymentMethod faltante/rechazado o validación de campos",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ReservationResponse updateStatus(@Parameter(description = "Identificador UUID de la reserva") @PathVariable UUID id,
            @Valid @RequestBody UpdateReservationStatusRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return reservationService.updateStatus(id, request, jwt);
    }
}
