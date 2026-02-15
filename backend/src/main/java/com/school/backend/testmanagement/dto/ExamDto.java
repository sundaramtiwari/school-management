package com.school.backend.testmanagement.dto;

import com.school.backend.common.enums.ExamStatus;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;

@Data
@Builder
public class ExamDto {
    private Long id;
    private Long schoolId;
    private Long classId;
    private Long sessionId;
    private String name;
    private String examType;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean active;
    private ExamStatus status;
}
