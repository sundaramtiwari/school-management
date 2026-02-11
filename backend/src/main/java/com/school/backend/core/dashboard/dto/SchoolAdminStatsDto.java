package com.school.backend.core.dashboard.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class SchoolAdminStatsDto {
    private long totalStudents;
    private long feePendingCount;
    private long transportCount;
    private long totalTeachers;
    private double attendancePercentage;
    private List<UpcomingExamDto> upcomingExams;

    @Data
    @Builder
    public static class UpcomingExamDto {
        private String name;
        private String date;
        private String className;
    }
}
