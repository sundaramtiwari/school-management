package com.school.backend.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
        private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

        @ExceptionHandler(ResourceNotFoundException.class)
        public ResponseEntity<Map<String, Object>> handleResourceNotFound(ResourceNotFoundException ex) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(Map.of(
                                                "timestamp", LocalDateTime.now(),
                                                "status", HttpStatus.NOT_FOUND.value(),
                                                "error", "Not Found",
                                                "message", ex.getMessage()));
        }

        @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
        public ResponseEntity<Map<String, Object>> handleOptimisticLockingFailure(
                        ObjectOptimisticLockingFailureException ex) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(Map.of(
                                                "timestamp", LocalDateTime.now(),
                                                "status", HttpStatus.CONFLICT.value(),
                                                "error", "Conflict",
                                                "message",
                                                "The record was updated by another user. Please refresh and try again."));
        }

        @ExceptionHandler(BusinessException.class)
        public ResponseEntity<Map<String, Object>> handleBusinessException(BusinessException ex) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(Map.of(
                                                "timestamp", LocalDateTime.now(),
                                                "status", HttpStatus.BAD_REQUEST.value(),
                                                "error", "Bad Request",
                                                "message", ex.getMessage()));
        }

        @ExceptionHandler(SubscriptionConcurrencyException.class)
        public ResponseEntity<Map<String, Object>> handleSubscriptionConcurrency(SubscriptionConcurrencyException ex) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(Map.of(
                                                "timestamp", LocalDateTime.now(),
                                                "status", HttpStatus.CONFLICT.value(),
                                                "error", "Conflict",
                                                "message", ex.getMessage()));
        }

        @ExceptionHandler(SubscriptionRuleViolationException.class)
        public ResponseEntity<Map<String, Object>> handleSubscriptionRuleViolation(
                        SubscriptionRuleViolationException ex) {
                return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                                .body(Map.of(
                                                "timestamp", LocalDateTime.now(),
                                                "status", HttpStatus.PAYMENT_REQUIRED.value(),
                                                "error", "Payment Required",
                                                "message", ex.getMessage()));
        }

        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(Map.of(
                                                "timestamp", LocalDateTime.now(),
                                                "status", HttpStatus.BAD_REQUEST.value(),
                                                "error", "Bad Request",
                                                "message", ex.getMessage()));
        }

        @ExceptionHandler(InvalidOperationException.class)
        public ResponseEntity<Map<String, Object>> handleInvalidOperation(InvalidOperationException ex) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(Map.of(
                                                "timestamp", LocalDateTime.now(),
                                                "status", HttpStatus.BAD_REQUEST.value(),
                                                "error", "Invalid Operation",
                                                "message", ex.getMessage()));
        }

        @ExceptionHandler(IllegalStateException.class)
        public ResponseEntity<Map<String, Object>> handleIllegalStateException(IllegalStateException ex) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(Map.of(
                                                "timestamp", LocalDateTime.now(),
                                                "status", HttpStatus.BAD_REQUEST.value(),
                                                "error", "Bad Request",
                                                "message", ex.getMessage()));
        }

        @ExceptionHandler(AccessDeniedException.class)
        public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body(Map.of(
                                                "timestamp", LocalDateTime.now(),
                                                "status", HttpStatus.FORBIDDEN.value(),
                                                "error", "Forbidden",
                                                "message",
                                                "Access Denied: You do not have permission to perform this action"));
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<Map<String, Object>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
                String message = ex.getBindingResult()
                                .getFieldErrors()
                                .stream()
                                .map(this::formatFieldError)
                                .collect(Collectors.joining(", "));

                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(Map.of(
                                                "status", HttpStatus.BAD_REQUEST.value(),
                                                "message", message));
        }

        @ExceptionHandler(DataIntegrityViolationException.class)
        public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(Map.of(
                                                "status", HttpStatus.BAD_REQUEST.value(),
                                                "message", "Duplicate or invalid data."));
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex, HttpServletRequest request) {
                String correlationId = request.getHeader("X-Correlation-Id");
                if (correlationId == null || correlationId.isBlank()) {
                        Object requestCorrelationId = request.getAttribute("correlationId");
                        correlationId = requestCorrelationId != null ? requestCorrelationId.toString()
                                        : UUID.randomUUID().toString();
                }

                log.error("Unhandled exception. correlationId={}", correlationId, ex);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(Map.of(
                                                "timestamp", LocalDateTime.now(),
                                                "status", HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                                "error", "Internal Server Error",
                                                "message",
                                                "An unexpected error occurred. Please contact support with correlationId: "
                                                                + correlationId));
        }

        private String formatFieldError(FieldError error) {
                String field = error.getField();
                String defaultMessage = error.getDefaultMessage() != null ? error.getDefaultMessage() : "Invalid value";
                return field + ": " + defaultMessage;
        }
}
