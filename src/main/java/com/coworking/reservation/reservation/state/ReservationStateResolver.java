package com.coworking.reservation.reservation.state;

import com.coworking.reservation.domain.enums.ReservationStatus;

import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class ReservationStateResolver {

    private final Map<ReservationStatus, ReservationState> states = new EnumMap<>(ReservationStatus.class);

    public ReservationStateResolver(List<ReservationState> reservationStates) {
        reservationStates.forEach(state -> states.put(state.currentStatus(), state));
    }

    public ReservationState resolve(ReservationStatus status) {
        return states.get(status);
    }
}
