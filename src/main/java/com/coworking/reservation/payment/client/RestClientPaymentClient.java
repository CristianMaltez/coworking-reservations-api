package com.coworking.reservation.payment.client;

import com.coworking.reservation.payment.dto.PaymentValidationRequest;
import com.coworking.reservation.payment.dto.PaymentValidationResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class RestClientPaymentClient implements PaymentClient {

    private final RestClient paymentRestClient;

    public RestClientPaymentClient(RestClient paymentRestClient) {
        this.paymentRestClient = paymentRestClient;
    }

    @Override
    public PaymentValidationResponse validate(PaymentValidationRequest request) {
        return paymentRestClient.post()
                .uri("/validate")
                .body(request)
                .retrieve()
                .body(PaymentValidationResponse.class);
    }
}
