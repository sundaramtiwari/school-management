package com.school.backend.common.exception;

public class SubscriptionConcurrencyException extends BusinessException {
    public SubscriptionConcurrencyException(String message) {
        super(message);
    }
}
