package com.coworking.reservation.reservation.state;

import com.coworking.reservation.domain.enums.ReservationStatus;

import org.springframework.stereotype.Component;

@Component
public class PendingPaymentReservationState implements ReservationState {

    @Override
    public ReservationStatus currentStatus() {
        return ReservationStatus.PENDING_PAYMENT;
    }

    @Override
    public boolean canTransitionTo(ReservationStatus targetStatus) {
        return targetStatus == ReservationStatus.CONFIRMED || targetStatus == ReservationStatus.CANCELLED;
    }
}
