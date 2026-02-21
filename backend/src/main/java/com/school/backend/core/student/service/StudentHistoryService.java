package com.school.backend.core.student.service;

import com.school.backend.common.exception.ResourceNotFoundException;
import com.school.backend.core.student.dto.StudentEnrollmentDto;
import com.school.backend.core.student.entity.StudentEnrollment;
import com.school.backend.core.student.repository.StudentEnrollmentRepository;
import com.school.backend.core.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import com.school.backend.core.classsubject.entity.SchoolClass;
import com.school.backend.core.classsubject.repository.SchoolClassRepository;
import com.school.backend.core.student.dto.PromotionRecordDto;
import com.school.backend.core.student.entity.PromotionRecord;
import com.school.backend.core.student.repository.PromotionRecordRepository;
import com.school.backend.school.entity.AcademicSession;
import com.school.backend.school.repository.AcademicSessionRepository;
import com.school.backend.user.security.SecurityUtil;

import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentHistoryService {

    private final StudentEnrollmentRepository enrollmentRepo;
    private final StudentRepository studentRepo;
    private final PromotionRecordRepository promotionRecordRepository;
    private final AcademicSessionRepository academicSessionRepository;
    private final SchoolClassRepository schoolClassRepository;

    // ================= Promotion History =================
    @Transactional(readOnly = true)
    public List<PromotionRecordDto> getPromotionHistory(Long studentId) {

        Long schoolId = SecurityUtil.schoolId();
        if (studentRepo.findByIdAndSchoolId(studentId, schoolId).isEmpty()) {
            throw new ResourceNotFoundException("Student not found or doesn't belong to current school: " + studentId);
        }

        List<PromotionRecord> records = promotionRecordRepository
                .findByStudentIdAndSchoolIdOrderByPromotedAtAsc(studentId, schoolId);

        // Pre-fetch session names and class names
        Map<Long, String> sessionNames = academicSessionRepository.findAll().stream()
                .collect(Collectors.toMap(AcademicSession::getId, AcademicSession::getName));

        // For simplicity, we fetch all classes for this school to map names.
        // SchoolClassRepository has findBySchoolId(Long schoolId, Pageable pageable)
        Map<Long, String> allClassNames = schoolClassRepository
                .findBySchoolId(schoolId, org.springframework.data.domain.Pageable.unpaged()).stream()
                .collect(Collectors.toMap(SchoolClass::getId,
                        c -> c.getName() + (c.getSection() != null ? " " + c.getSection() : "")));

        return records.stream().map(r -> {
            PromotionRecordDto dto = new PromotionRecordDto();
            dto.setId(r.getId());
            dto.setStudentId(r.getStudentId());
            dto.setSourceSessionId(r.getSourceSessionId());
            dto.setSourceSessionName(sessionNames.get(r.getSourceSessionId()));
            dto.setTargetSessionId(r.getTargetSessionId());
            dto.setTargetSessionName(sessionNames.get(r.getTargetSessionId()));
            dto.setSourceClassId(r.getSourceClassId());
            dto.setSourceClassName(allClassNames.get(r.getSourceClassId()));
            dto.setTargetClassId(r.getTargetClassId());
            dto.setTargetClassName(allClassNames.get(r.getTargetClassId()));
            dto.setPromotionType(r.getPromotionType());
            dto.setRemarks(r.getRemarks());
            dto.setPromotedBy(r.getPromotedBy());
            dto.setPromotedAt(r.getPromotedAt());
            return dto;
        }).toList();
    }

    // ================= Enrollment History =================
    @Transactional(readOnly = true)
    public List<StudentEnrollmentDto> getEnrollmentHistory(Long studentId) {

        if (!studentRepo.existsById(studentId)) {
            throw new ResourceNotFoundException("Student not found: " + studentId);
        }

        return enrollmentRepo.findByStudentIdOrderBySessionIdAsc(studentId)
                .stream()
                .map(this::toEnrollmentDto)
                .toList();
    }

    // ================= Mapping =================
    private StudentEnrollmentDto toEnrollmentDto(StudentEnrollment e) {
        StudentEnrollmentDto dto = new StudentEnrollmentDto();
        dto.setId(e.getId());
        dto.setStudentId(e.getStudentId());
        dto.setClassId(e.getClassId());
        dto.setSection(e.getSection());
        dto.setSessionId(e.getSessionId());
        dto.setRollNumber(e.getRollNumber());
        dto.setEnrollmentDate(e.getEnrollmentDate());
        dto.setActive(e.isActive());
        dto.setRemarks(e.getRemarks());
        return dto;
    }
}
