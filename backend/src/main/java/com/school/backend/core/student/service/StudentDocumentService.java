package com.school.backend.core.student.service;

import com.school.backend.common.dto.PageResponse;
import com.school.backend.common.dto.PageResponseMapper;
import com.school.backend.common.exception.ResourceNotFoundException;
import com.school.backend.core.student.dto.StudentDocumentCreateRequest;
import com.school.backend.core.student.dto.StudentDocumentDto;
import com.school.backend.core.student.entity.StudentDocument;
import com.school.backend.core.student.repository.StudentDocumentRepository;
import com.school.backend.core.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class StudentDocumentService {

    private final StudentDocumentRepository docRepo;
    private final StudentRepository studentRepo;

    // -------------------- CREATE --------------------
    @Transactional
    public StudentDocumentDto create(StudentDocumentCreateRequest req) {
        if (!studentRepo.existsById(req.getStudentId())) {
            throw new ResourceNotFoundException("Student not found: " + req.getStudentId());
        }

        StudentDocument doc = StudentDocument.builder()
                .studentId(req.getStudentId())
                .fileType(req.getFileType())
                .fileName(req.getFileName())
                .fileUrl(req.getFileUrl())
                .uploadedAt(LocalDateTime.now())
                .remarks(req.getRemarks())
                .build();

        return toDto(docRepo.save(doc));
    }

    // -------------------- LIST (PAGINATED) --------------------
    @Transactional(readOnly = true)
    public PageResponse<StudentDocumentDto> listByStudent(Long studentId, Pageable pageable) {

        if (!studentRepo.existsById(studentId)) {
            throw new ResourceNotFoundException("Student not found: " + studentId);
        }

        Page<StudentDocument> page = docRepo.findByStudentId(studentId, pageable);

        Page<StudentDocumentDto> mapped = page.map(this::toDto);

        return PageResponseMapper.fromPage(mapped);
    }

    // -------------------- DELETE --------------------
    @Transactional
    public void delete(Long studentId, Long documentId) {
        StudentDocument doc = docRepo.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + documentId));

        if (!doc.getStudentId().equals(studentId)) {
            throw new ResourceNotFoundException("Document does not belong to student " + studentId);
        }

        docRepo.delete(doc);
    }

    // -------------------- MAPPER --------------------
    private StudentDocumentDto toDto(StudentDocument doc) {
        StudentDocumentDto dto = new StudentDocumentDto();
        dto.setId(doc.getId());
        dto.setStudentId(doc.getStudentId());
        dto.setFileType(doc.getFileType());
        dto.setFileName(doc.getFileName());
        dto.setFileUrl(doc.getFileUrl());
        dto.setUploadedAt(doc.getUploadedAt());
        dto.setRemarks(doc.getRemarks());
        return dto;
    }
}
