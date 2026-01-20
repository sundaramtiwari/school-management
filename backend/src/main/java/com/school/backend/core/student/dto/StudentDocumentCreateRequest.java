package com.school.backend.core.student.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StudentDocumentCreateRequest {

    @NotNull
    private Long studentId;

    @NotBlank
    private String fileType;

    @NotBlank
    private String fileName;

    @NotBlank
    private String fileUrl;

    private String remarks;
}
