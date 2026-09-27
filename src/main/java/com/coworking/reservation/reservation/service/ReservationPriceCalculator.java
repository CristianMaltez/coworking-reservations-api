package com.coworking.reservation.reservation.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.OffsetDateTime;

@Component
public class ReservationPriceCalculator {

    private static final BigDecimal SECONDS_PER_HOUR = BigDecimal.valueOf(3600);
    private static final BigDecimal NANOS_PER_SECOND = BigDecimal.valueOf(1_000_000_000);

    public BigDecimal calculate(BigDecimal hourlyRate, OffsetDateTime startTime, OffsetDateTime endTime) {
        Duration duration = Duration.between(startTime, endTime);
        BigDecimal seconds = BigDecimal.valueOf(duration.getSeconds())
                .add(BigDecimal.valueOf(duration.getNano()).divide(NANOS_PER_SECOND, 9, RoundingMode.HALF_UP));
        BigDecimal hours = seconds.divide(SECONDS_PER_HOUR, 8, RoundingMode.HALF_UP);
        return hourlyRate.multiply(hours).setScale(2, RoundingMode.HALF_UP);
    }
}
