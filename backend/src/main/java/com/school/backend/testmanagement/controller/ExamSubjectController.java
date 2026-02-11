package com.school.backend.testmanagement.controller;

import com.school.backend.testmanagement.dto.ExamSubjectCreateRequest;
import com.school.backend.testmanagement.entity.ExamSubject;
import com.school.backend.testmanagement.service.ExamSubjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exam-subjects")
@RequiredArgsConstructor
public class ExamSubjectController {

    private final ExamSubjectService service;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PLATFORM_ADMIN', 'SCHOOL_ADMIN')")
    public ExamSubject create(@RequestBody ExamSubjectCreateRequest req) {
        return service.create(req);
    }

    @GetMapping("/by-exam/{examId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PLATFORM_ADMIN', 'SCHOOL_ADMIN', 'TEACHER')")
    public List<ExamSubject> listByExam(@PathVariable Long examId) {
        return service.listByExam(examId);
    }
}
