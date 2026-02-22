package com.school.backend.core.classsubject.controller;

import com.school.backend.core.classsubject.dto.ClassSubjectAssignmentDto;
import com.school.backend.core.classsubject.dto.ClassSubjectDto;
import com.school.backend.core.classsubject.entity.SchoolClass;
import com.school.backend.core.classsubject.entity.Subject;
import com.school.backend.core.classsubject.service.ClassSubjectService;
import com.school.backend.user.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/class-subjects")
@RequiredArgsConstructor
public class ClassSubjectController {

    private final ClassSubjectService service;

    @PostMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ClassSubjectDto> create(@RequestBody ClassSubjectDto dto) {
        dto.setSchoolId(SecurityUtil.schoolId());
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

    @GetMapping("/mine")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'ACCOUNTANT', 'SUPER_ADMIN')")
    public ResponseEntity<Page<ClassSubjectDto>> bySchool(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(service.getBySchool(SecurityUtil.schoolId(), pageable));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/assignments")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'PLATFORM_ADMIN', 'SUPER_ADMIN')")
    public ClassSubjectAssignmentDto assignTeacher(@RequestBody ClassSubjectAssignmentDto req) {
        return service.assignTeacher(req.getTeacherId(), req.getSessionId(), req.getClassId(), req.getSubjectId());
    }

    @GetMapping("/assignments")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'PLATFORM_ADMIN', 'SUPER_ADMIN')")
    public List<ClassSubjectAssignmentDto> listAssignments(
            @RequestParam(required = false) Long sessionId,
            @RequestParam(required = false) Long teacherId) {
        return service.listAssignments(sessionId, teacherId);
    }

    @DeleteMapping("/assignments/{id}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'PLATFORM_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Void> deactivateAssignment(@PathVariable Long id) {
        service.deactivateAssignment(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/my-classes")
    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN', 'PLATFORM_ADMIN', 'SUPER_ADMIN')")
    public List<SchoolClass> getMyAssignedClasses(@RequestParam Long sessionId) {
        return service.getMyAssignedClasses(sessionId);
    }

    @GetMapping("/my-subjects")
    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN', 'PLATFORM_ADMIN', 'SUPER_ADMIN')")
    public List<Subject> getMyAssignedSubjects(@RequestParam Long sessionId, @RequestParam Long classId) {
        return service.getMyAssignedSubjects(sessionId, classId);
    }
}
