package com.coworking.reservation.payment.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentValidationRequest(
        UUID reservationId,
        UUID userId,
        BigDecimal amount,
        String paymentMethod
) {
}
