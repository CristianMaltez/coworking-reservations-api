package com.coworking.reservation.repository;

import com.coworking.reservation.domain.entity.Reservation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    boolean existsBySpaceId(UUID spaceId);
}
