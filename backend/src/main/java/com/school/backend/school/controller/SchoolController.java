package com.school.backend.school.controller;

import com.school.backend.common.dto.PageResponse;
import com.school.backend.common.dto.PageResponseMapper;
import com.school.backend.school.dto.SchoolDto;
import com.school.backend.school.dto.SchoolOnboardingRequest;
import com.school.backend.school.service.SchoolService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/schools")
@RequiredArgsConstructor
public class SchoolController {

    private final SchoolService schoolService;

    /**
     * Create - returns 201 Created with Location header
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<SchoolDto> create(@RequestBody SchoolDto schoolDto) {
        SchoolDto created = schoolService.create(schoolDto);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{code}")
                .buildAndExpand(created.getSchoolCode())
                .toUri();

        return ResponseEntity.created(location).body(created);
    }

    /**
     * Onboard School + Admin
     */
    @PostMapping("/onboard")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<SchoolDto> onboard(@RequestBody @Valid SchoolOnboardingRequest req) {
        SchoolDto created = schoolService.createSchoolWithAdmin(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Paginated list
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<PageResponse<SchoolDto>> getAll(Pageable pageable) {
        Page<SchoolDto> page = schoolService.listSchools(pageable);
        PageResponse<SchoolDto> response = PageResponseMapper.fromPage(page);
        return ResponseEntity.ok(response);
    }

    /**
     * Get by schoolCode
     */
    @GetMapping("/{code}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PLATFORM_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<SchoolDto> getByCode(@PathVariable String code) {
        return ResponseEntity.ok(schoolService.getByCode(code));
    }

    @GetMapping("/id/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PLATFORM_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<SchoolDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(schoolService.getById(id));
    }

    /**
     * PUT - full replace. Client should send full DTO (fields missing will be
     * overwritten).
     */
    @PutMapping("/{code}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<SchoolDto> replace(@PathVariable String code, @RequestBody SchoolDto schoolDto) {
        SchoolDto replaced = schoolService.replaceByCode(code, schoolDto);
        return ResponseEntity.ok(replaced);
    }

    /**
     * PATCH - partial update. Only non-null fields in DTO are applied.
     */
    @PatchMapping("/{code}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<SchoolDto> update(@PathVariable String code, @RequestBody SchoolDto schoolDto) {
        SchoolDto updated = schoolService.updateByCode(code, schoolDto);
        return ResponseEntity.ok(updated);
    }
}
