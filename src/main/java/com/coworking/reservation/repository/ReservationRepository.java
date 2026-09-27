package com.coworking.reservation.repository;

import com.coworking.reservation.domain.entity.Reservation;
import com.coworking.reservation.domain.enums.ReservationStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, UUID>, JpaSpecificationExecutor<Reservation> {

    boolean existsBySpaceId(UUID spaceId);

    @Query("""
            select count(r) > 0
            from Reservation r
            where r.space.id = :spaceId
              and r.status in :blockingStatuses
              and r.startTime < :requestedEnd
              and r.endTime > :requestedStart
            """)
    boolean existsBlockingOverlap(UUID spaceId, OffsetDateTime requestedStart, OffsetDateTime requestedEnd,
            Collection<ReservationStatus> blockingStatuses);

    @EntityGraph(attributePaths = {"user", "space"})
    @Query("select r from Reservation r where r.id = :id")
    Optional<Reservation> findWithUserAndSpaceById(UUID id);

    @Override
    @EntityGraph(attributePaths = {"user", "space"})
    Page<Reservation> findAll(Specification<Reservation> specification, Pageable pageable);
}
