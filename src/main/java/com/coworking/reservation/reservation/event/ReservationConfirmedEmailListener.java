package com.coworking.reservation.reservation.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ReservationConfirmedEmailListener {

    private static final Logger logger = LoggerFactory.getLogger(ReservationConfirmedEmailListener.class);

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendConfirmationEmail(ReservationConfirmedEvent event) {
        logger.info("Correo de confirmación simulado enviado a {} para la reserva {}",
                event.userEmail(), event.reservationId());
    }
}
