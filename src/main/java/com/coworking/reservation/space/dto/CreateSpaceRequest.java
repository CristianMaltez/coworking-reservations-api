package com.coworking.reservation.space.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateSpaceRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre no debe superar 150 caracteres")
        String name,
        @NotNull(message = "El tipo de espacio es obligatorio")
        @Positive(message = "El tipo de espacio debe ser positivo")
        Short spaceTypeId,
        @NotNull(message = "La capacidad es obligatoria")
        @Positive(message = "La capacidad debe ser positiva")
        Integer capacity,
        @NotBlank(message = "La ubicación es obligatoria")
        @Size(max = 255, message = "La ubicación no debe superar 255 caracteres")
        String location,
        @NotNull(message = "La tarifa por hora es obligatoria")
        @DecimalMin(value = "0.00", message = "La tarifa por hora debe ser mayor o igual a 0.00")
        @Digits(integer = 8, fraction = 2, message = "La tarifa por hora debe tener hasta 8 dígitos enteros y 2 decimales")
        BigDecimal hourlyRate
) {
}
