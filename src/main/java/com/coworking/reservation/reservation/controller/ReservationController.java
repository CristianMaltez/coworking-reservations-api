package com.coworking.reservation.reservation.controller;

import com.coworking.reservation.reservation.dto.CreateReservationRequest;
import com.coworking.reservation.reservation.dto.ReservationPageResponse;
import com.coworking.reservation.reservation.dto.ReservationResponse;
import com.coworking.reservation.reservation.dto.ReservationSearchCriteria;
import com.coworking.reservation.reservation.dto.UpdateReservationStatusRequest;
import com.coworking.reservation.reservation.service.ReservationService;

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
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    public ResponseEntity<ReservationResponse> create(@Valid @RequestBody CreateReservationRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        ReservationResponse response = reservationService.create(request, jwt);
        return ResponseEntity.created(URI.create("/api/reservations/" + response.id())).body(response);
    }

    @GetMapping
    public ReservationPageResponse findAll(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) UUID spaceId,
            @RequestParam(required = false) Short statusCode,
            @RequestParam(required = false) OffsetDateTime from,
            @RequestParam(required = false) OffsetDateTime to,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @AuthenticationPrincipal Jwt jwt) {
        return reservationService.findAll(new ReservationSearchCriteria(userId, spaceId, statusCode, from, to, page, size), jwt);
    }

    @GetMapping("/{id}")
    public ReservationResponse findById(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return reservationService.findById(id, jwt);
    }

    @PatchMapping("/{id}")
    public ReservationResponse updateStatus(@PathVariable UUID id,
            @Valid @RequestBody UpdateReservationStatusRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return reservationService.updateStatus(id, request, jwt);
    }
}
