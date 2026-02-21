package com.school.backend.core.teacher.dto;

import lombok.Data;

@Data
public class TeacherListItemDto {
    private Long id;
    private Long userId;
    private String fullName;
}
