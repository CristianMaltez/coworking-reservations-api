package com.coworking.reservation.reservation.state;

import com.coworking.reservation.domain.enums.ReservationStatus;

import org.springframework.stereotype.Component;

@Component
public class ConfirmedReservationState implements ReservationState {

    @Override
    public ReservationStatus currentStatus() {
        return ReservationStatus.CONFIRMED;
    }

    @Override
    public boolean canTransitionTo(ReservationStatus targetStatus) {
        return targetStatus == ReservationStatus.CANCELLED;
    }
}
