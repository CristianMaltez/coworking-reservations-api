package com.coworking.reservation.payment.client;

import com.coworking.reservation.payment.dto.PaymentValidationRequest;
import com.coworking.reservation.payment.dto.PaymentValidationResponse;

public interface PaymentClient {

    PaymentValidationResponse validate(PaymentValidationRequest request);
}
