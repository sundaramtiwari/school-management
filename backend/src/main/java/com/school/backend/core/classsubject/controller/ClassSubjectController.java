package com.school.backend.core.classsubject.controller;

import com.school.backend.core.classsubject.dto.ClassSubjectDto;
import com.school.backend.core.classsubject.service.ClassSubjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/class-subjects")
@RequiredArgsConstructor
public class ClassSubjectController {

    private final ClassSubjectService service;

    @PostMapping
    public ResponseEntity<ClassSubjectDto> create(@RequestBody ClassSubjectDto dto) {
        return ResponseEntity.ok(service.create(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClassSubjectDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping("/by-class/{classId}")
    public ResponseEntity<Page<ClassSubjectDto>> byClass(
            @PathVariable Long classId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
    
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(service.getByClass(classId, pageable));
    }

    @GetMapping("/by-school/{schoolId}")
    public ResponseEntity<Page<ClassSubjectDto>> bySchool(
            @PathVariable Long schoolId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(service.getBySchool(schoolId, pageable));
    }

    @GetMapping
    public ResponseEntity<Page<ClassSubjectDto>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(service.getAll(pageable));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
