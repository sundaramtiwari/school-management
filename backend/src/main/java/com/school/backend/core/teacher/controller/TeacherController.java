package com.school.backend.core.teacher.controller;

import com.school.backend.core.teacher.dto.TeacherListItemDto;
import com.school.backend.core.teacher.service.TeacherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TeacherController {

    private final TeacherService teacherService;

    @GetMapping("/teachers")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'PLATFORM_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<List<TeacherListItemDto>> listTeachers() {
        return ResponseEntity.ok(teacherService.listTeachers());
    }
}
