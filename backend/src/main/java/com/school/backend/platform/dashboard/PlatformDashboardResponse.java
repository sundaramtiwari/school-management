package com.school.backend.platform.dashboard;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PlatformDashboardResponse {
    private long totalSchools;
    private long totalStudents;
    private long totalTeachers;
    private long totalActiveSessions;
    private long subscriptionDueCount;
}
