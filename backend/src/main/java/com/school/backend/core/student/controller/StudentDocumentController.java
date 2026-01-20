package com.school.backend.core.student.controller;

import com.school.backend.common.dto.PageResponse;
import com.school.backend.core.student.dto.StudentDocumentCreateRequest;
import com.school.backend.core.student.dto.StudentDocumentDto;
import com.school.backend.core.student.service.StudentDocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/students/{studentId}/documents")
@RequiredArgsConstructor
public class StudentDocumentController {

    private final StudentDocumentService service;

    @PostMapping
    public ResponseEntity<StudentDocumentDto> create(
            @PathVariable Long studentId,
            @Valid @RequestBody StudentDocumentCreateRequest req) {

        req.setStudentId(studentId);
        return ResponseEntity.ok(service.create(req));
    }

    @GetMapping
    public PageResponse<StudentDocumentDto> listDocuments(
            @PathVariable Long studentId,
            Pageable pageable
    ) {
        return service.listByStudent(studentId, pageable);
    }

    @DeleteMapping("/{documentId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long studentId,
            @PathVariable Long documentId) {

        service.delete(studentId, documentId);
        return ResponseEntity.noContent().build();
    }
}
