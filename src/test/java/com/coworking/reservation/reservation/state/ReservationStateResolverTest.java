package com.coworking.reservation.reservation.state;

import static com.coworking.reservation.domain.enums.ReservationStatus.CANCELLED;
import static com.coworking.reservation.domain.enums.ReservationStatus.CONFIRMED;
import static com.coworking.reservation.domain.enums.ReservationStatus.PENDING_PAYMENT;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import java.util.List;

class ReservationStateResolverTest {

    private final ReservationStateResolver resolver = new ReservationStateResolver(List.of(
            new PendingPaymentReservationState(),
            new ConfirmedReservationState(),
            new CancelledReservationState()
    ));

    @Test
    void pendingPaymentCanTransitionToConfirmed() {
        assertThat(resolver.resolve(PENDING_PAYMENT).canTransitionTo(CONFIRMED)).isTrue();
    }

    @Test
    void pendingPaymentCanTransitionToCancelled() {
        assertThat(resolver.resolve(PENDING_PAYMENT).canTransitionTo(CANCELLED)).isTrue();
    }

    @Test
    void confirmedCanTransitionToCancelled() {
        assertThat(resolver.resolve(CONFIRMED).canTransitionTo(CANCELLED)).isTrue();
    }

    @Test
    void cancelledCannotTransitionToConfirmed() {
        assertThat(resolver.resolve(CANCELLED).canTransitionTo(CONFIRMED)).isFalse();
    }

    @Test
    void cancelledCannotTransitionToPendingPayment() {
        assertThat(resolver.resolve(CANCELLED).canTransitionTo(PENDING_PAYMENT)).isFalse();
    }
}
