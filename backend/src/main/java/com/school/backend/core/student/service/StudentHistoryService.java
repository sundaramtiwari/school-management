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

@Service
@RequiredArgsConstructor
public class StudentHistoryService {

    private final StudentEnrollmentRepository enrollmentRepo;
    private final StudentRepository studentRepo;

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
