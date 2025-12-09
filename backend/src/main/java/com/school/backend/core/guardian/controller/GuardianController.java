package com.school.backend.core.guardian.controller;

import com.school.backend.common.dto.PageResponse;
import com.school.backend.common.dto.PageResponseMapper;
import com.school.backend.core.guardian.dto.GuardianCreateRequest;
import com.school.backend.core.guardian.dto.GuardianDto;
import com.school.backend.core.guardian.service.GuardianService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/guardians")
@RequiredArgsConstructor
public class GuardianController {

    private final GuardianService service;

    @PostMapping
    public ResponseEntity<GuardianDto> create(@Valid @RequestBody GuardianCreateRequest req) {
        return ResponseEntity.ok(service.create(req));
    }

    @GetMapping("/by-school/{schoolId}")
    public ResponseEntity<PageResponse<GuardianDto>> bySchool(
            @PathVariable Long schoolId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<GuardianDto> p = service.listBySchool(schoolId, pageable);
        return ResponseEntity.ok(PageResponseMapper.fromPage(p));
    }
}
