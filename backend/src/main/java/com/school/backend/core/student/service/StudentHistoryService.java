package com.school.backend.core.student.service;

import com.school.backend.common.exception.ResourceNotFoundException;
import com.school.backend.core.classsubject.entity.SchoolClass;
import com.school.backend.core.classsubject.repository.SchoolClassRepository;
import com.school.backend.core.student.dto.PromotionRecordDto;
import com.school.backend.core.student.dto.PromotionRequest;
import com.school.backend.core.student.dto.StudentEnrollmentDto;
import com.school.backend.core.student.entity.PromotionRecord;
import com.school.backend.core.student.entity.Student;
import com.school.backend.core.student.entity.StudentEnrollment;
import com.school.backend.core.student.repository.PromotionRecordRepository;
import com.school.backend.core.student.repository.StudentEnrollmentRepository;
import com.school.backend.core.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentHistoryService {

    private final StudentEnrollmentRepository enrollmentRepo;
    private final PromotionRecordRepository promotionRepo;
    private final StudentRepository studentRepo;
    private final SchoolClassRepository classRepo;

    // ================= Enrollment History =================
    @Transactional(readOnly = true)
    public List<StudentEnrollmentDto> getEnrollmentHistory(Long studentId) {

        if (!studentRepo.existsById(studentId)) {
            throw new ResourceNotFoundException("Student not found: " + studentId);
        }

        return enrollmentRepo.findByStudentIdOrderBySessionAsc(studentId)
                .stream()
                .map(this::toEnrollmentDto)
                .toList();
    }

    // ================= Promotion History =================
    @Transactional(readOnly = true)
    public List<PromotionRecordDto> getPromotionHistory(Long studentId) {

        if (!studentRepo.existsById(studentId)) {
            throw new ResourceNotFoundException("Student not found: " + studentId);
        }

        return promotionRepo.findByStudentIdOrderBySessionAsc(studentId)
                .stream()
                .map(this::toPromotionDto)
                .toList();
    }

    // ================= Promote Student =================
    @Transactional
    public PromotionRecordDto promoteStudent(Long studentId, PromotionRequest req) {

        Student student = studentRepo.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentId));

        // ---- Extract current class info ----
        SchoolClass currentClass = student.getCurrentClass();
        Long fromClassId = null;
        String fromSection = null;

        if (currentClass != null) {
            fromClassId = currentClass.getId();
            fromSection = currentClass.getSection();
        }

        // ---- Validate new class ----
        SchoolClass toClass = classRepo.findById(req.getToClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found: " + req.getToClassId()));

        // --------- Create PromotionRecord ---------
        PromotionRecord record = PromotionRecord.builder()
                .studentId(studentId)
                .fromClassId(fromClassId)
                .fromSection(fromSection)
                .toClassId(req.getToClassId())
                .toSection(req.getToSection())
                .session(req.getSession())
                .promotedOn(LocalDate.now())
                .promoted(req.isPromoted())
                .feePending(req.isFeePending())
                .remarks(req.getRemarks())
                .build();

        PromotionRecord savedRecord = promotionRepo.save(record);

        // --------- Create new enrollment entry ---------
        StudentEnrollment enrollment = StudentEnrollment.builder()
                .studentId(studentId)
                .classId(req.getToClassId())
                .section(req.getToSection())
                .session(req.getSession())
                .rollNumber(null)
                .enrollmentDate(LocalDate.now())
                .active(true)
                .remarks(req.getRemarks())
                .build();

        enrollmentRepo.save(enrollment);

        // --------- Update student's current class ---------
        student.setCurrentClass(toClass);
        studentRepo.save(student);

        return toPromotionDto(savedRecord);
    }

    // ================= Mapping =================
    private StudentEnrollmentDto toEnrollmentDto(StudentEnrollment e) {
        StudentEnrollmentDto dto = new StudentEnrollmentDto();
        dto.setId(e.getId());
        dto.setStudentId(e.getStudentId());
        dto.setClassId(e.getClassId());
        dto.setSection(e.getSection());
        dto.setSession(e.getSession());
        dto.setRollNumber(e.getRollNumber());
        dto.setEnrollmentDate(e.getEnrollmentDate());
        dto.setActive(e.isActive());
        dto.setRemarks(e.getRemarks());
        return dto;
    }

    private PromotionRecordDto toPromotionDto(PromotionRecord p) {
        PromotionRecordDto dto = new PromotionRecordDto();
        dto.setId(p.getId());
        dto.setStudentId(p.getStudentId());
        dto.setFromClassId(p.getFromClassId());
        dto.setFromSection(p.getFromSection());
        dto.setToClassId(p.getToClassId());
        dto.setToSection(p.getToSection());
        dto.setSession(p.getSession());
        dto.setPromotedOn(p.getPromotedOn());
        dto.setPromoted(p.isPromoted());
        dto.setFeePending(p.isFeePending());
        dto.setRemarks(p.getRemarks());
        return dto;
    }
}
