package com.coworking.reservation.payment.mock;

import com.coworking.reservation.payment.dto.PaymentValidationRequest;
import com.coworking.reservation.payment.dto.PaymentValidationResponse;

import io.swagger.v3.oas.annotations.Hidden;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Hidden
@Profile("dev")
@RestController
@RequestMapping("/mock/payments")
public class MockPaymentController {

    @PostMapping("/validate")
    public PaymentValidationResponse validate(@RequestBody PaymentValidationRequest request) throws InterruptedException {
        return switch (request.paymentMethod()) {
            case "test-approved" -> new PaymentValidationResponse(true);
            case "test-rejected" -> new PaymentValidationResponse(false);
            case "test-error" -> throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error simulado de pagos");
            case "test-slow" -> slowResponse();
            default -> new PaymentValidationResponse(false);
        };
    }

    private PaymentValidationResponse slowResponse() throws InterruptedException {
        Thread.sleep(3_000);
        return new PaymentValidationResponse(true);
    }
}
