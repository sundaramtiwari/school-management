package com.school.backend.school.controller;

import com.school.backend.school.entity.AcademicSession;
import com.school.backend.school.service.AcademicSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.school.backend.user.security.SecurityUtil;

import java.util.List;

@RestController
@RequestMapping("/api/academic-sessions")
@RequiredArgsConstructor
public class AcademicSessionController {

    private final AcademicSessionService service;

    @GetMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'ACCOUNTANT', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public List<AcademicSession> getSessions() {
        return service.getSessions(SecurityUtil.schoolId());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public AcademicSession createSession(@RequestBody AcademicSession session) {
        session.setSchoolId(SecurityUtil.schoolId());
        return service.createSession(session);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public AcademicSession updateSession(@PathVariable Long id, @RequestBody AcademicSession session) {
        return service.updateSession(id, session);
    }

    @PutMapping("/{sessionId}/set-current")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public void setCurrentSession(@PathVariable Long sessionId) {
        service.setCurrentSession(SecurityUtil.schoolId(), sessionId);
    }
}
