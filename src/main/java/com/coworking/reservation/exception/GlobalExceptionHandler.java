package com.coworking.reservation.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> fields = new HashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> fields.put(error.getField(), error.getDefaultMessage()));
        return error(HttpStatus.UNPROCESSABLE_ENTITY, "Error de validación", fields);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableMessage(HttpMessageNotReadableException exception) {
        return error(HttpStatus.BAD_REQUEST, "Solicitud mal formada", Map.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        return error(HttpStatus.BAD_REQUEST, "Parámetro de solicitud inválido", Map.of());
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ApiError> handleDuplicateEmail(DuplicateEmailException exception) {
        return error(HttpStatus.CONFLICT, exception.getMessage(), Map.of());
    }

    @ExceptionHandler({BadCredentialsException.class, DisabledException.class})
    public ResponseEntity<ApiError> handleUnauthorized(RuntimeException exception) {
        return error(HttpStatus.UNAUTHORIZED, "Credenciales inválidas", Map.of());
    }

    @ExceptionHandler(AuthenticatedUserNotFoundException.class)
    public ResponseEntity<ApiError> handleAuthenticatedUserNotFound(AuthenticatedUserNotFoundException exception) {
        return error(HttpStatus.UNAUTHORIZED, "El usuario autenticado no está disponible", Map.of());
    }

    @ExceptionHandler(MissingCatalogRoleException.class)
    public ResponseEntity<ApiError> handleMissingRole(MissingCatalogRoleException exception) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(SpaceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(RuntimeException exception) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(SpaceTypeNotFoundException.class)
    public ResponseEntity<ApiError> handleUnprocessableEntity(SpaceTypeNotFoundException exception) {
        return error(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Error de validación",
                Map.of("spaceTypeId", "El tipo de espacio no existe")
        );
    }

    @ExceptionHandler(SpaceHasReservationsException.class)
    public ResponseEntity<ApiError> handleConflict(SpaceHasReservationsException exception) {
        return error(HttpStatus.CONFLICT, exception.getMessage(), Map.of());
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String message, Map<String, String> fields) {
        return ResponseEntity.status(status).body(new ApiError(
                OffsetDateTime.now(ZoneOffset.UTC),
                status.value(),
                errorReason(status),
                message,
                fields
        ));
    }

    private String errorReason(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> "Solicitud incorrecta";
            case UNAUTHORIZED -> "No autorizado";
            case FORBIDDEN -> "Prohibido";
            case NOT_FOUND -> "No encontrado";
            case CONFLICT -> "Conflicto";
            case UNPROCESSABLE_ENTITY -> "Contenido no procesable";
            case INTERNAL_SERVER_ERROR -> "Error interno del servidor";
            default -> "Error";
        };
    }
}
