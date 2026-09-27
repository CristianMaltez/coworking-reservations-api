package com.coworking.reservation.reservation.service;

import com.coworking.reservation.domain.entity.Reservation;
import com.coworking.reservation.domain.enums.ReservationStatus;

import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.UUID;

public final class ReservationSpecifications {

    private ReservationSpecifications() {
    }

    public static Specification<Reservation> userId(UUID userId) {
        return (root, query, builder) -> userId == null ? null : builder.equal(root.get("user").get("id"), userId);
    }

    public static Specification<Reservation> spaceId(UUID spaceId) {
        return (root, query, builder) -> spaceId == null ? null : builder.equal(root.get("space").get("id"), spaceId);
    }

    public static Specification<Reservation> status(ReservationStatus status) {
        return (root, query, builder) -> status == null ? null : builder.equal(root.get("status"), status);
    }

    public static Specification<Reservation> intersects(OffsetDateTime from, OffsetDateTime to) {
        return (root, query, builder) -> {
            if (from != null && to != null) {
                return builder.and(
                        builder.lessThan(root.get("startTime"), to),
                        builder.greaterThan(root.get("endTime"), from)
                );
            }
            if (from != null) {
                return builder.greaterThan(root.get("endTime"), from);
            }
            if (to != null) {
                return builder.lessThan(root.get("startTime"), to);
            }
            return null;
        };
    }
}
