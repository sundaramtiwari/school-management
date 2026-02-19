package com.school.backend.common.enums;

import lombok.Getter;

@Getter
public enum FeeFrequency {
    MONTHLY(12),
    QUARTERLY(4),
    HALF_YEARLY(2),
    ANNUALLY(1),
    ONE_TIME(1);

    private final int periodsPerYear;

    FeeFrequency(int periodsPerYear) {
        this.periodsPerYear = periodsPerYear;
    }

}
