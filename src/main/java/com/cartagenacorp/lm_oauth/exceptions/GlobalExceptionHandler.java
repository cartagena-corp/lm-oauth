package com.cartagenacorp.lm_oauth.exceptions;

import com.cartagenacorp.lm_oauth.dto.NotificationResponse;
import com.cartagenacorp.lm_oauth.util.ConstantUtil;
import com.cartagenacorp.lm_oauth.util.ResponseUtil;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<NotificationResponse> handleBaseException(BaseException ex) {
        return ResponseEntity.status(ex.getStatusCode())
                .body(ResponseUtil.error(ex.getMessage(), HttpStatus.valueOf(ex.getStatusCode())));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<NotificationResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        String combinedErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining(" | "));

        return ResponseEntity.badRequest()
                .body(ResponseUtil.error(combinedErrors, HttpStatus.BAD_REQUEST));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<NotificationResponse> handleResponseStatusException(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode())
                .body(ResponseUtil.error(ex.getReason(), (HttpStatus) ex.getStatusCode()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<NotificationResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ResponseUtil.error(ConstantUtil.DATA_INTEGRITY_FAIL_MESSAGE, HttpStatus.CONFLICT));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<NotificationResponse> handleEntityNotFound(EntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ResponseUtil.error(ConstantUtil.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<NotificationResponse> handleIllegalArgument(IllegalArgumentException ex) {
        String message = ex.getMessage() != null && ex.getMessage().toLowerCase().contains("uuid")
                ? ConstantUtil.INVALID_UUID
                : ConstantUtil.INVALID_INPUT;

        return ResponseEntity.badRequest()
                .body(ResponseUtil.error(message, HttpStatus.BAD_REQUEST));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<NotificationResponse> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ResponseUtil.error(ConstantUtil.PERMISSION_DENIED, HttpStatus.FORBIDDEN));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<NotificationResponse> handleGenericException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResponseUtil.error(ConstantUtil.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR));
    }
}
