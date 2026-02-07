package com.school.backend.core.student.controller;

import com.school.backend.common.dto.PageResponse;
import com.school.backend.common.dto.PageResponseMapper;
import com.school.backend.core.student.dto.StudentEnrollmentDto;
import com.school.backend.core.student.dto.StudentEnrollmentRequest;
import com.school.backend.core.student.service.EnrollmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService service;

    @PostMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'ACCOUNTANT', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<StudentEnrollmentDto> enroll(@Valid @RequestBody StudentEnrollmentRequest req) {
        return ResponseEntity.ok(service.enroll(req));
    }

    @GetMapping("/by-class/{classId}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'ACCOUNTANT', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<PageResponse<StudentEnrollmentDto>> byClass(
            @PathVariable Long classId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<StudentEnrollmentDto> p = service.listByClass(classId, pageable);
        return ResponseEntity.ok(PageResponseMapper.fromPage(p));
    }
}
