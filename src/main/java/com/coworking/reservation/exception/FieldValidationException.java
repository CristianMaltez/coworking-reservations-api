package com.coworking.reservation.exception;

import java.util.Map;

public class FieldValidationException extends RuntimeException {

    private final Map<String, String> fields;

    public FieldValidationException(Map<String, String> fields) {
        super("Error de validación");
        this.fields = Map.copyOf(fields);
    }

    public Map<String, String> getFields() {
        return fields;
    }
}
