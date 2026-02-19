package com.school.backend.core.teacher.controller;

import com.school.backend.core.teacher.dto.TeacherAssignmentListItemDto;
import com.school.backend.core.teacher.dto.TeacherAssignmentRequest;
import com.school.backend.core.teacher.entity.TeacherAssignment;
import com.school.backend.core.teacher.service.TeacherAssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TeacherAssignmentController {

    private final TeacherAssignmentService service;

    @PostMapping("/teacher-assignments")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'PLATFORM_ADMIN', 'SUPER_ADMIN')")
    public TeacherAssignment assignTeacher(@RequestBody TeacherAssignmentRequest req) {
        return service.assignTeacher(req.getTeacherId(), req.getSessionId(), req.getClassId(), req.getSubjectId());
    }

    @DeleteMapping("/teacher-assignments/{id}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'PLATFORM_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Void> deactivateAssignment(@PathVariable Long id) {
        service.deactivateAssignment(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/teacher-assignments")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'PLATFORM_ADMIN', 'SUPER_ADMIN')")
    public List<TeacherAssignmentListItemDto> listAssignments(@RequestParam Long sessionId) {
        return service.listBySession(sessionId);
    }

    @GetMapping("/teachers/{id}/assignments")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'PLATFORM_ADMIN', 'SUPER_ADMIN')")
    public List<TeacherAssignment> getTeacherAssignments(
            @PathVariable Long id,
            @RequestParam Long sessionId) {
        return service.getTeacherAssignments(id, sessionId);
    }
}
