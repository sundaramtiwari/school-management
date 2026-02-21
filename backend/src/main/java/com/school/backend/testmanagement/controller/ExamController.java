package com.school.backend.testmanagement.controller;

import com.school.backend.testmanagement.dto.BulkMarksDto;
import com.school.backend.testmanagement.dto.ExamCreateRequest;
import com.school.backend.testmanagement.entity.Exam;
import com.school.backend.testmanagement.service.ExamService;
import com.school.backend.common.tenant.SessionResolver;
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
    private final SessionResolver sessionResolver;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PLATFORM_ADMIN', 'SCHOOL_ADMIN', 'TEACHER')")
    public Exam create(@Valid @RequestBody ExamCreateRequest req) {
        return service.create(req);
    }

    @GetMapping("/by-class/{classId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PLATFORM_ADMIN', 'SCHOOL_ADMIN', 'TEACHER')")
    public List<Exam> listByClass(
            @PathVariable Long classId,
            @RequestParam(required = false) Long sessionId) {

        Long effectiveSessionId = sessionId != null ? sessionId : sessionResolver.resolveForCurrentSchool();
        return service.listByClass(classId, effectiveSessionId);
    }

    @PostMapping("/{examId}/marks/bulk")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PLATFORM_ADMIN', 'SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<com.school.backend.testmanagement.dto.BulkMarkEntryResponse> saveMarksBulk(
            @PathVariable Long examId,
            @RequestBody BulkMarksDto dto) {
        return ResponseEntity.ok(service.saveMarksBulk(examId, dto));
    }

    @PutMapping("/{examId}/publish")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PLATFORM_ADMIN', 'SCHOOL_ADMIN', 'TEACHER')")
    public com.school.backend.testmanagement.dto.ExamDto publish(@PathVariable Long examId) {
        return service.publishExam(examId);
    }

    @PutMapping("/{examId}/lock")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PLATFORM_ADMIN', 'SCHOOL_ADMIN')")
    public com.school.backend.testmanagement.dto.ExamDto lock(@PathVariable Long examId) {
        return service.lockExam(examId);
    }
}
