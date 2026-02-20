package com.school.backend.core.teacher.controller;

import com.school.backend.core.classsubject.entity.SchoolClass;
import com.school.backend.core.classsubject.entity.Subject;
import com.school.backend.core.teacher.dto.TeacherAssignmentListItemDto;
import com.school.backend.core.teacher.dto.TeacherAssignmentRequest;
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
    public TeacherAssignmentListItemDto assignTeacher(@RequestBody TeacherAssignmentRequest req) {
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
    public List<TeacherAssignmentListItemDto> getTeacherAssignments(
            @PathVariable Long id,
            @RequestParam Long sessionId) {
        return service.getTeacherAssignments(id, sessionId);
    }

    @GetMapping("/teacher-assignments/my-classes")
    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN', 'PLATFORM_ADMIN', 'SUPER_ADMIN')")
    public List<SchoolClass> getMyAssignedClasses(@RequestParam Long sessionId) {
        return service.getMyAssignedClasses(sessionId);
    }

    @GetMapping("/teacher-assignments/my-subjects")
    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN', 'PLATFORM_ADMIN', 'SUPER_ADMIN')")
    public List<Subject> getMyAssignedSubjects(@RequestParam Long sessionId, @RequestParam Long classId) {
        return service.getMyAssignedSubjects(sessionId, classId);
    }
}
