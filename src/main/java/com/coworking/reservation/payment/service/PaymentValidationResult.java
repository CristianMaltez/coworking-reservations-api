package com.coworking.reservation.payment.service;

public record PaymentValidationResult(
        PaymentValidationStatus status
) {

    public static PaymentValidationResult approved() {
        return new PaymentValidationResult(PaymentValidationStatus.APPROVED);
    }

    public static PaymentValidationResult rejected() {
        return new PaymentValidationResult(PaymentValidationStatus.REJECTED);
    }

    public static PaymentValidationResult unavailable() {
        return new PaymentValidationResult(PaymentValidationStatus.PAYMENT_SERVICE_UNAVAILABLE);
    }
}
