package com.coworking.reservation.space.service;

import com.coworking.reservation.domain.entity.Space;
import com.coworking.reservation.domain.entity.SpaceType;
import com.coworking.reservation.exception.SpaceHasReservationsException;
import com.coworking.reservation.exception.SpaceNotFoundException;
import com.coworking.reservation.exception.SpaceTypeNotFoundException;
import com.coworking.reservation.repository.ReservationRepository;
import com.coworking.reservation.repository.SpaceRepository;
import com.coworking.reservation.repository.SpaceTypeRepository;
import com.coworking.reservation.space.dto.CreateSpaceRequest;
import com.coworking.reservation.space.dto.SpaceResponse;
import com.coworking.reservation.space.dto.SpaceTypeResponse;
import com.coworking.reservation.space.dto.UpdateSpaceRequest;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class SpaceService {

    private final SpaceRepository spaceRepository;
    private final SpaceTypeRepository spaceTypeRepository;
    private final ReservationRepository reservationRepository;

    public SpaceService(SpaceRepository spaceRepository, SpaceTypeRepository spaceTypeRepository,
            ReservationRepository reservationRepository) {
        this.spaceRepository = spaceRepository;
        this.spaceTypeRepository = spaceTypeRepository;
        this.reservationRepository = reservationRepository;
    }

    @Transactional(readOnly = true)
    public List<SpaceResponse> findAll() {
        return spaceRepository.findAllWithSpaceType().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SpaceResponse findById(UUID id) {
        return spaceRepository.findWithSpaceTypeById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new SpaceNotFoundException(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public SpaceResponse create(CreateSpaceRequest request) {
        SpaceType spaceType = resolveSpaceType(request.spaceTypeId());
        Space space = new Space(request.name(), request.capacity(), request.location(), request.hourlyRate(), spaceType);
        return toResponse(spaceRepository.save(space));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public SpaceResponse update(UUID id, UpdateSpaceRequest request) {
        Space space = spaceRepository.findWithSpaceTypeById(id)
                .orElseThrow(() -> new SpaceNotFoundException(id));
        SpaceType spaceType = resolveSpaceType(request.spaceTypeId());
        space.update(request.name(), request.capacity(), request.location(), request.hourlyRate(), spaceType);
        return toResponse(space);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void delete(UUID id) {
        Space space = spaceRepository.findById(id)
                .orElseThrow(() -> new SpaceNotFoundException(id));

        if (reservationRepository.existsBySpaceId(id)) {
            throw new SpaceHasReservationsException(id);
        }

        spaceRepository.delete(space);
    }

    private SpaceType resolveSpaceType(Short id) {
        return spaceTypeRepository.findById(id)
                .orElseThrow(() -> new SpaceTypeNotFoundException(id));
    }

    private SpaceResponse toResponse(Space space) {
        SpaceType spaceType = space.getSpaceType();
        return new SpaceResponse(
                space.getId(),
                space.getName(),
                space.getCapacity(),
                space.getLocation(),
                space.getHourlyRate(),
                new SpaceTypeResponse(spaceType.getId(), spaceType.getCode(), spaceType.getName()),
                space.getCreatedAt(),
                space.getUpdatedAt()
        );
    }
}
