package com.school.backend.core.guardian.controller;

import com.school.backend.common.dto.PageResponse;
import com.school.backend.common.dto.PageResponseMapper;
import com.school.backend.core.guardian.dto.GuardianCreateRequest;
import com.school.backend.core.guardian.dto.GuardianDto;
import com.school.backend.core.guardian.service.GuardianService;
import com.school.backend.core.student.dto.StudentGuardianDto;
import com.school.backend.core.student.service.StudentService;
import com.school.backend.user.security.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/guardians")
@RequiredArgsConstructor
public class GuardianController {

    private final GuardianService service;
    private final StudentService studentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'SUPER_ADMIN')")
    public ResponseEntity<GuardianDto> create(@Valid @RequestBody GuardianCreateRequest req) {
        return ResponseEntity.ok(service.create(req));
    }

    @GetMapping("/mine")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'ACCOUNTANT', 'SUPER_ADMIN')")
    public ResponseEntity<PageResponse<GuardianDto>> bySchool(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<GuardianDto> p = service.listBySchool(SecurityUtil.schoolId(), pageable);
        return ResponseEntity.ok(PageResponseMapper.fromPage(p));
    }

    @GetMapping("/{id}/guardians")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'ACCOUNTANT', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<List<StudentGuardianDto>> getGuardians(@PathVariable Long id) {
        return ResponseEntity.ok(studentService.getGuardiansForStudent(id));
    }

    @PutMapping("/{id}/guardians")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<Void> replaceGuardians(
            @PathVariable Long id,
            @RequestBody List<GuardianCreateRequest> guardians) {
        studentService.replaceGuardians(id, guardians);
        return ResponseEntity.noContent().build();
    }
}
