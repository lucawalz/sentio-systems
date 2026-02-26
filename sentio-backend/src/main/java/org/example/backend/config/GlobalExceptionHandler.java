package org.example.backend.config;

import org.example.backend.dto.ErrorResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Global exception handler for the Sentio backend.
 *
 * <p>
 * Catches {@link MethodArgumentNotValidException} thrown when a
 * {@code @Valid}-annotated request body fails bean validation, and returns
 * a structured 400 response with field-level error details.
 * </p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles validation errors from {@code @Valid @RequestBody} parameters.
     * Collects all field errors into a human-readable message and returns
     * the existing {@link ErrorResponseDTO} format with a 400 status.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidationException(MethodArgumentNotValidException ex) {
        String fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));

        log.warn("Validation failed: {}", fieldErrors);

        ErrorResponseDTO errorResponse = ErrorResponseDTO.builder()
                .error("Validation Failed")
                .message(fieldErrors)
                .action("Please correct the invalid fields and try again.")
                .status(HttpStatus.BAD_REQUEST.value())
                .build();

        return ResponseEntity.badRequest().body(errorResponse);
    }
}
