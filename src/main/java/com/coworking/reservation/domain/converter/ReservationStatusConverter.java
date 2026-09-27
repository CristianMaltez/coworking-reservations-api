package com.coworking.reservation.domain.converter;

import com.coworking.reservation.domain.enums.ReservationStatus;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class ReservationStatusConverter implements AttributeConverter<ReservationStatus, Short> {

    @Override
    public Short convertToDatabaseColumn(ReservationStatus attribute) {
        return attribute == null ? null : attribute.getCode();
    }

    @Override
    public ReservationStatus convertToEntityAttribute(Short dbData) {
        return dbData == null ? null : ReservationStatus.fromCode(dbData);
    }
}
