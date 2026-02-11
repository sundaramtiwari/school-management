package com.school.backend.testmanagement.controller;

import com.school.backend.testmanagement.dto.MarkEntryRequest;
import com.school.backend.testmanagement.entity.StudentMark;
import com.school.backend.testmanagement.service.MarkEntryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/marks")
@RequiredArgsConstructor
public class MarkEntryController {

    private final MarkEntryService service;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PLATFORM_ADMIN', 'SCHOOL_ADMIN', 'TEACHER')")
    public StudentMark enter(@RequestBody MarkEntryRequest req) {
        return service.enterMarks(req);
    }

    @GetMapping("/exam/{examId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PLATFORM_ADMIN', 'SCHOOL_ADMIN', 'TEACHER')")
    public List<StudentMark> listByExam(@PathVariable Long examId) {
        return service.getMarksByExam(examId);
    }
}
