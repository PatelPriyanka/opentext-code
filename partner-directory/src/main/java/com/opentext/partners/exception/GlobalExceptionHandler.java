package com.opentext.partners.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // -------------------------------
    // 1. Handle WebClient HTTP Errors
    // -------------------------------
    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<Map<String, Object>> handleWebClientResponseException(WebClientResponseException ex) {
        log.error("WebClient HTTP error: ", ex);

        Map<String, Object> error = createError(
                "HTTP error while calling external API",
                ex.getStatusCode().value(),
                ex.getResponseBodyAsString()
        );

        return new ResponseEntity<>(error, ex.getStatusCode());
    }

    // ---------------------------------------------
    // 2. Handle WebClient Request/Connection Errors
    // ---------------------------------------------
    @ExceptionHandler(WebClientRequestException.class)
    public ResponseEntity<Map<String, Object>> handleWebClientRequestException(WebClientRequestException ex) {
        log.error("WebClient request error: ", ex);

        Map<String, Object> error = createError(
                "Failed to connect to external API",
                HttpStatus.GATEWAY_TIMEOUT.value(),
                ex.getMessage()
        );

        return new ResponseEntity<>(error, HttpStatus.GATEWAY_TIMEOUT);
    }

    // ----------------------------
    // 3. Handle Illegal Arguments
    // ----------------------------
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        log.error("Invalid argument: ", ex);

        Map<String, Object> error = createError(
                "Invalid input",
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage()
        );

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    // -----------------------------------
    // 4. Fallback for Any Internal Error
    // -----------------------------------
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(Exception ex) {
        log.error("Unexpected internal error: ", ex);

        Map<String, Object> error = createError(
                "Internal server error",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                ex.getMessage()
        );

        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // ----------------------
    // COMMON ERROR RESPONSE
    // ----------------------
    private Map<String, Object> createError(String message, int status, String details) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status);
        body.put("message", message);
        body.put("details", details);
        return body;
    }
}
