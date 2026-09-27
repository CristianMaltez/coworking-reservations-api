package com.coworking.reservation.reservation.state;

import com.coworking.reservation.domain.enums.ReservationStatus;

public interface ReservationState {

    ReservationStatus currentStatus();

    boolean canTransitionTo(ReservationStatus targetStatus);
}
