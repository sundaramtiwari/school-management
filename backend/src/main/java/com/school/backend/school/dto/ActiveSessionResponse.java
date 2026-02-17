package com.school.backend.school.dto;

public record ActiveSessionResponse(
        boolean exists,
        Long sessionId,
        String name) {
}
