package com.school.backend.core.classsubject.controller;

import com.school.backend.core.classsubject.dto.SchoolClassDto;
import com.school.backend.core.classsubject.service.SchoolClassService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.school.backend.user.security.SecurityUtil;

@RestController
@RequestMapping("/api/classes")
@RequiredArgsConstructor
public class SchoolClassController {

    private final SchoolClassService service;

    @PostMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<SchoolClassDto> create(@RequestBody SchoolClassDto dto) {
        dto.setSchoolId(SecurityUtil.schoolId());
        return ResponseEntity.ok(service.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<SchoolClassDto> update(@PathVariable Long id, @RequestBody SchoolClassDto dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'ACCOUNTANT', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<SchoolClassDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping("/mine")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'ACCOUNTANT', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<Page<SchoolClassDto>> getBySchool(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(service.getBySchool(SecurityUtil.schoolId(), pageable));
    }

    @GetMapping("/mine/session/{session}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'ACCOUNTANT', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<Page<SchoolClassDto>> getBySchoolAndSession(
            @PathVariable String session,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(service.getBySchoolAndSession(SecurityUtil.schoolId(), session, pageable));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<Page<SchoolClassDto>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        return ResponseEntity.ok(service.getAll(pageable));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
