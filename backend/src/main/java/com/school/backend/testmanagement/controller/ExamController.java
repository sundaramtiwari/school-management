package com.school.backend.testmanagement.controller;

import com.school.backend.testmanagement.dto.BulkMarksDto;
import com.school.backend.testmanagement.dto.ExamCreateRequest;
import com.school.backend.testmanagement.entity.Exam;
import com.school.backend.testmanagement.service.ExamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exams")
@RequiredArgsConstructor
public class ExamController {

    private final ExamService service;

    @PostMapping
    public Exam create(@Valid @RequestBody ExamCreateRequest req) {
        return service.create(req);
    }

    @GetMapping("/by-class/{classId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PLATFORM_ADMIN', 'SCHOOL_ADMIN', 'TEACHER')")
    public List<Exam> listByClass(
            @PathVariable Long classId,
            @RequestParam String session) {

        return service.listByClass(classId, session);
    }

    @PostMapping("/{examId}/marks/bulk")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PLATFORM_ADMIN', 'SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<Void> saveMarksBulk(
            @PathVariable Long examId,
            @RequestBody BulkMarksDto dto) {
        service.saveMarksBulk(examId, dto);
        return ResponseEntity.ok().build();
    }
}
