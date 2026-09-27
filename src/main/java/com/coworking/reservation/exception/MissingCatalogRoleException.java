package com.coworking.reservation.exception;

public class MissingCatalogRoleException extends RuntimeException {

    public MissingCatalogRoleException(String roleCode) {
        super("El rol requerido no está configurado: " + roleCode);
    }
}
