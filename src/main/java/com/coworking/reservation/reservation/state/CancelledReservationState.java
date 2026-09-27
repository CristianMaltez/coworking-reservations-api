package com.coworking.reservation.reservation.state;

import com.coworking.reservation.domain.enums.ReservationStatus;

import org.springframework.stereotype.Component;

@Component
public class CancelledReservationState implements ReservationState {

    @Override
    public ReservationStatus currentStatus() {
        return ReservationStatus.CANCELLED;
    }

    @Override
    public boolean canTransitionTo(ReservationStatus targetStatus) {
        return false;
    }
}
