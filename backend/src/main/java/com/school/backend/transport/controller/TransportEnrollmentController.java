package com.school.backend.transport.controller;

import com.school.backend.transport.dto.TransportEnrollmentDto;
import com.school.backend.transport.service.TransportEnrollmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transport/enrollments")
@RequiredArgsConstructor
public class TransportEnrollmentController {

    private final TransportEnrollmentService enrollmentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PLATFORM_ADMIN', 'SCHOOL_ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<TransportEnrollmentDto> enroll(@RequestBody TransportEnrollmentDto dto) {
        return ResponseEntity.ok(enrollmentService.enrollStudent(dto));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PLATFORM_ADMIN', 'SCHOOL_ADMIN', 'ACCOUNTANT', 'TEACHER')")
    public ResponseEntity<TransportEnrollmentDto> getByStudent(
            @PathVariable Long studentId,
            @RequestParam Long sessionId) {
        return enrollmentService.getStudentEnrollment(studentId, sessionId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @GetMapping("/active-status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PLATFORM_ADMIN', 'SCHOOL_ADMIN', 'ACCOUNTANT', 'TEACHER')")
    public ResponseEntity<java.util.List<TransportEnrollmentDto>> getBatchStatus(
            @RequestParam java.util.Collection<Long> studentIds,
            @RequestParam Long sessionId) {
        return ResponseEntity.ok(enrollmentService.getActiveEnrollmentsForStudents(studentIds, sessionId));
    }

    @DeleteMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PLATFORM_ADMIN', 'SCHOOL_ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<Void> unenroll(
            @PathVariable Long studentId,
            @RequestParam Long sessionId) {
        enrollmentService.unenrollStudent(studentId, sessionId);
        return ResponseEntity.ok().build();
    }
}
