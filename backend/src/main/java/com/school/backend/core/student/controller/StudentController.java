package com.school.backend.core.student.controller;

import com.school.backend.common.dto.PageResponse;
import com.school.backend.common.dto.PageResponseMapper;
import com.school.backend.core.student.dto.StudentCreateRequest;
import com.school.backend.core.student.dto.StudentDto;
import com.school.backend.core.student.dto.StudentUpdateRequest;
import com.school.backend.core.student.service.StudentService;
import com.school.backend.user.security.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService service;

    @PostMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<StudentDto> register(@Valid @RequestBody StudentCreateRequest req) {
        req.setSchoolId(SecurityUtil.schoolId());
        return ResponseEntity.ok(service.register(req));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'ACCOUNTANT', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<StudentDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping("/mine")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'ACCOUNTANT', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<PageResponse<StudentDto>> bySchool(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<StudentDto> p = service.listBySchool(SecurityUtil.schoolId(), pageable);
        return ResponseEntity.ok(PageResponseMapper.fromPage(p));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<StudentDto> update(
            @PathVariable Long id,
            @Valid @RequestBody StudentUpdateRequest req) {
        return ResponseEntity.ok(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/by-class/{classId}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'ACCOUNTANT', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<PageResponse<StudentDto>> byClass(
            @PathVariable Long classId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("firstName").ascending());
        Page<StudentDto> p = service.listByClass(classId, pageable);
        return ResponseEntity.ok(PageResponseMapper.fromPage(p));
    }

}
