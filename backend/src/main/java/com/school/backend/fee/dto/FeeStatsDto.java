package com.school.backend.fee.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeeStatsDto {
    private long todayCollection;
    private long pendingDues;
    private long totalStudents;
}
