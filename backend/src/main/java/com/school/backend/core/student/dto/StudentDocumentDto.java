package com.school.backend.core.student.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StudentDocumentDto {
    private Long id;
    private Long studentId;
    private String fileType;
    private String fileName;
    private String fileUrl;
    private LocalDateTime uploadedAt;
    private String remarks;
}
