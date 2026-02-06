package com.school.backend.school.controller;

import com.school.backend.school.entity.AcademicSession;
import com.school.backend.school.service.AcademicSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/academic-sessions")
@RequiredArgsConstructor
public class AcademicSessionController {

    private final AcademicSessionService service;

    @GetMapping
    public List<AcademicSession> getSessions(@RequestParam Long schoolId) {
        return service.getSessions(schoolId);
    }

    @PostMapping
    public AcademicSession createSession(@RequestBody AcademicSession session) {
        return service.createSession(session);
    }

    @PutMapping("/{id}")
    public AcademicSession updateSession(@PathVariable Long id, @RequestBody AcademicSession session) {
        return service.updateSession(id, session);
    }
}
