package com.school.backend.fee.dto;

import lombok.Data;

@Data
public class FeeTypePatchRequest {
    private String name;

    // Immutable field: reject if payload tries to mutate tenant ownership.
    private Long schoolId;
}
