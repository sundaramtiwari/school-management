package com.school.backend.core.student.controller;

import com.school.backend.core.student.dto.GuardianLinkRequest;
import com.school.backend.core.student.dto.StudentGuardianDto;
import com.school.backend.core.student.service.StudentGuardianService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/students/{studentId}/guardians")
@RequiredArgsConstructor
public class StudentGuardianController {

    private final StudentGuardianService service;

    @PostMapping
    public ResponseEntity<StudentGuardianDto> link(@PathVariable Long studentId,
                                                   @Valid @RequestBody GuardianLinkRequest req) {
        StudentGuardianDto dto = service.linkGuardian(studentId, req.getGuardianId(), req.isPrimaryGuardian());
        return ResponseEntity.ok(dto);
    }

    @GetMapping
    public ResponseEntity<List<StudentGuardianDto>> list(@PathVariable Long studentId) {
        return ResponseEntity.ok(service.listGuardiansForStudent(studentId));
    }

    @DeleteMapping("/{guardianId}")
    public ResponseEntity<Void> unlink(@PathVariable Long studentId, @PathVariable Long guardianId) {
        service.unlinkGuardian(studentId, guardianId);
        return ResponseEntity.noContent().build();
    }
}
