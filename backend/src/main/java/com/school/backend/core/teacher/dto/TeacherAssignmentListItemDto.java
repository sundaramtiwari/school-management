package com.school.backend.core.teacher.dto;

import lombok.Data;

@Data
public class TeacherAssignmentListItemDto {
    private Long id;
    private String teacherName;
    private String className;
    private String subjectName;
    private String sessionName;
    private String status;
}
