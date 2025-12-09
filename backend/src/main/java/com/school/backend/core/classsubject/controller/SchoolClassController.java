package com.school.backend.core.classsubject.controller;

import com.school.backend.core.classsubject.dto.SchoolClassDto;
import com.school.backend.core.classsubject.service.SchoolClassService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/classes")
@RequiredArgsConstructor
public class SchoolClassController {

    private final SchoolClassService service;

    @PostMapping
    public ResponseEntity<SchoolClassDto> create(@RequestBody SchoolClassDto dto) {
        return ResponseEntity.ok(service.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SchoolClassDto> update(@PathVariable Long id, @RequestBody SchoolClassDto dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SchoolClassDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping("/by-school/{schoolId}")
    public ResponseEntity<Page<SchoolClassDto>> getBySchool(
            @PathVariable Long schoolId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(service.getBySchool(schoolId, pageable));
    }

    @GetMapping("/by-school/{schoolId}/session/{session}")
    public ResponseEntity<Page<SchoolClassDto>> getBySchoolAndSession(
            @PathVariable Long schoolId,
            @PathVariable String session,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(service.getBySchoolAndSession(schoolId, session, pageable));
    }

    @GetMapping
    public ResponseEntity<Page<SchoolClassDto>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        return ResponseEntity.ok(service.getAll(pageable));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
