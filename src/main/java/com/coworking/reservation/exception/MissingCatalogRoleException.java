package com.coworking.reservation.exception;

public class MissingCatalogRoleException extends RuntimeException {

    public MissingCatalogRoleException(String roleCode) {
        super("Required role is not configured: " + roleCode);
    }
}
