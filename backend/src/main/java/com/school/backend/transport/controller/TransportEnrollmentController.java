package com.school.backend.transport.controller;

import com.school.backend.transport.dto.TransportEnrollmentDto;
import com.school.backend.transport.service.TransportEnrollmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;

@RestController
@RequestMapping("/api/transport/enrollments")
@RequiredArgsConstructor
public class TransportEnrollmentController {

    private final TransportEnrollmentService enrollmentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SCHOOL_ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<TransportEnrollmentDto> enroll(@RequestBody TransportEnrollmentDto dto) {
        return ResponseEntity.ok(enrollmentService.enrollStudent(dto));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PLATFORM_ADMIN', 'SCHOOL_ADMIN', 'ACCOUNTANT', 'TEACHER')")
    public ResponseEntity<TransportEnrollmentDto> getByStudent(@PathVariable Long studentId) {
        return enrollmentService.getStudentEnrollment(studentId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @GetMapping("/active-status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PLATFORM_ADMIN', 'SCHOOL_ADMIN', 'ACCOUNTANT', 'TEACHER')")
    public ResponseEntity<List<TransportEnrollmentDto>> getBatchStatus(@RequestParam Collection<Long> studentIds) {
        return ResponseEntity.ok(enrollmentService.getActiveEnrollmentsForStudents(studentIds));
    }

    @DeleteMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SCHOOL_ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<Void> unenroll(@PathVariable Long studentId) {
        enrollmentService.unenrollStudent(studentId);
        return ResponseEntity.ok().build();
    }
}
