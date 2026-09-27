package com.coworking.reservation.payment.service;

import com.coworking.reservation.payment.client.PaymentClient;
import com.coworking.reservation.payment.dto.PaymentValidationRequest;
import com.coworking.reservation.payment.dto.PaymentValidationResponse;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PaymentValidationService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentValidationService.class);

    private final PaymentClient paymentClient;

    public PaymentValidationService(PaymentClient paymentClient) {
        this.paymentClient = paymentClient;
    }

    @CircuitBreaker(name = "paymentService", fallbackMethod = "paymentFallback")
    public PaymentValidationResult validate(PaymentValidationRequest request) {
        PaymentValidationResponse response = paymentClient.validate(request);
        return response != null && response.approved()
                ? PaymentValidationResult.approved()
                : PaymentValidationResult.rejected();
    }

    public PaymentValidationResult paymentFallback(PaymentValidationRequest request, Throwable exception) {
        logger.warn("Servicio de pagos no disponible para la reserva {}: {}",
                request.reservationId(), exception.getMessage());
        return PaymentValidationResult.unavailable();
    }
}
