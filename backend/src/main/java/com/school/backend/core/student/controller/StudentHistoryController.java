package com.school.backend.core.student.controller;

import com.school.backend.core.student.dto.StudentEnrollmentDto;
import com.school.backend.core.student.service.StudentHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/students/{studentId}/history")
@RequiredArgsConstructor
public class StudentHistoryController {

    private final StudentHistoryService service;

    // ----------------- Enrollment Timeline -----------------
    @GetMapping("/enrollments")
    public ResponseEntity<List<StudentEnrollmentDto>> getEnrollments(
            @PathVariable Long studentId) {

        return ResponseEntity.ok(service.getEnrollmentHistory(studentId));
    }

}
