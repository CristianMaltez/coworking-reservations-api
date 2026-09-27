package com.coworking.reservation.catalog.controller;

import com.coworking.reservation.domain.enums.ReservationStatus;
import com.coworking.reservation.repository.SpaceTypeRepository;
import com.coworking.reservation.reservation.dto.ReservationStatusResponse;
import com.coworking.reservation.space.dto.SpaceTypeResponse;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/catalogs")
public class CatalogController {

    private final SpaceTypeRepository spaceTypeRepository;

    public CatalogController(SpaceTypeRepository spaceTypeRepository) {
        this.spaceTypeRepository = spaceTypeRepository;
    }

    @GetMapping("/reservation-statuses")
    public List<ReservationStatusResponse> reservationStatuses() {
        return Arrays.stream(ReservationStatus.values())
                .map(ReservationStatusResponse::from)
                .toList();
    }

    @GetMapping("/space-types")
    public List<SpaceTypeResponse> spaceTypes() {
        return spaceTypeRepository.findAllByOrderByIdAsc().stream()
                .map(spaceType -> new SpaceTypeResponse(spaceType.getId(), spaceType.getCode(), spaceType.getName()))
                .toList();
    }
}
