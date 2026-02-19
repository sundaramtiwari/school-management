package com.school.backend.core.teacher.dto;

import lombok.Data;

@Data
public class TeacherAssignmentRequest {
    private Long teacherId;
    private Long sessionId;
    private Long classId;
    private Long subjectId;
}
