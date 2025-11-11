package com.school.backend.school.controller;

import com.school.backend.school.entity.School;
import com.school.backend.school.service.SchoolService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/schools")
@RequiredArgsConstructor
public class SchoolController {

    private final SchoolService schoolService;

    @PostMapping
    public ResponseEntity<School> create(@RequestBody School school) {
        return ResponseEntity.ok(schoolService.create(school));
    }

    @GetMapping
    public ResponseEntity<List<School>> getAll() {
        return ResponseEntity.ok(schoolService.getAll());
    }

    @GetMapping("/{code}")
    public ResponseEntity<School> getByCode(@PathVariable String code) {
        return ResponseEntity.ok(schoolService.getByCode(code));
    }
}
