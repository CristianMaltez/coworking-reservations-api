package com.coworking.reservation.exception;

public class SpaceTypeNotFoundException extends RuntimeException {

    private final Short spaceTypeId;

    public SpaceTypeNotFoundException(Short spaceTypeId) {
        super("Tipo de espacio no encontrado: " + spaceTypeId);
        this.spaceTypeId = spaceTypeId;
    }

    public Short getSpaceTypeId() {
        return spaceTypeId;
    }
}
