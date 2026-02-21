package com.school.backend.core.student.controller;

import com.school.backend.core.student.dto.StudentEnrollmentDto;
import com.school.backend.core.student.service.StudentHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/students/{studentId}/history")
@RequiredArgsConstructor
public class StudentHistoryController {

    private final StudentHistoryService service;

    // ----------------- Enrollment Timeline -----------------
    @GetMapping("/enrollments")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SCHOOL_ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<List<StudentEnrollmentDto>> getEnrollments(
            @PathVariable Long studentId) {

        return ResponseEntity.ok(service.getEnrollmentHistory(studentId));
    }

    @GetMapping("/promotions")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SCHOOL_ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<List<com.school.backend.core.student.dto.PromotionRecordDto>> getPromotions(
            @PathVariable Long studentId) {

        return ResponseEntity.ok(service.getPromotionHistory(studentId));
    }

}
