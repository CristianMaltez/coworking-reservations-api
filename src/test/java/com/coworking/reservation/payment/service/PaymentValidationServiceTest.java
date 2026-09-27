package com.coworking.reservation.payment.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.coworking.reservation.payment.dto.PaymentValidationRequest;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

class PaymentValidationServiceTest {

    @Test
    void fallbackReturnsUnavailableInsteadOfPropagatingExternalFailure() {
        PaymentValidationService service = new PaymentValidationService(request -> {
            throw new IllegalStateException("boom");
        });
        PaymentValidationRequest request = new PaymentValidationRequest(
                UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("40.00"), "test-error");

        PaymentValidationResult result = service.paymentFallback(request, new IllegalStateException("boom"));

        assertThat(result.status()).isEqualTo(PaymentValidationStatus.PAYMENT_SERVICE_UNAVAILABLE);
    }
}
