package com.school.backend.core.student.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class StudentWithdrawalResponse {
    private boolean enrollmentClosed;
    private long futureAssignmentsDeactivated;
    private long futureAssignmentsSkippedDueToPayment;
    private List<Long> skippedAssignmentIds;
    private boolean transportUnenrolled;
}
