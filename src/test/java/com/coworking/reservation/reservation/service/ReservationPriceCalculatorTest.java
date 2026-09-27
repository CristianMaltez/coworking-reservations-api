package com.coworking.reservation.reservation.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

class ReservationPriceCalculatorTest {

    private final ReservationPriceCalculator calculator = new ReservationPriceCalculator();

    @Test
    void calculatesPriceForTwoAndHalfHours() {
        OffsetDateTime start = OffsetDateTime.of(2027, 1, 10, 10, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime end = start.plusHours(2).plusMinutes(30);

        BigDecimal total = calculator.calculate(new BigDecimal("20.00"), start, end);

        assertThat(total).isEqualByComparingTo("50.00");
    }

    @Test
    void roundsFractionalShortDurationUsingProductionRoundingMode() {
        OffsetDateTime start = OffsetDateTime.of(2027, 1, 10, 10, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime end = start.plusMinutes(1);

        BigDecimal total = calculator.calculate(new BigDecimal("10.00"), start, end);

        assertThat(total).isEqualByComparingTo("0.17");
    }
}
