package com.school.backend.core.classsubject.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassSubjectAssignmentDto {
    private Long id; // ClassSubject ID
    private Long teacherId;
    private String teacherName;
    private Long classId;
    private String className;
    private Long subjectId;
    private String subjectName;
    private Long sessionId;
    private String sessionName;
}
