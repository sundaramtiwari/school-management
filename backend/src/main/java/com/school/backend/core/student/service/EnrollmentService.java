package com.school.backend.core.student.service;

import com.school.backend.common.exception.ResourceNotFoundException;
import com.school.backend.core.classsubject.repository.SchoolClassRepository;
import com.school.backend.core.student.dto.PromotionRequest;
import com.school.backend.core.student.dto.StudentEnrollmentDto;
import com.school.backend.core.student.dto.StudentEnrollmentRequest;
import com.school.backend.core.student.entity.PromotionRecord;
import com.school.backend.core.student.entity.StudentEnrollment;
import com.school.backend.core.student.mapper.StudentEnrollmentMapper;
import com.school.backend.core.student.repository.PromotionRecordRepository;
import com.school.backend.core.student.repository.StudentEnrollmentRepository;
import com.school.backend.core.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final StudentEnrollmentRepository enrollmentRepository;
    private final StudentEnrollmentMapper enrollmentMapper;
    private final StudentRepository studentRepository;
    private final SchoolClassRepository classRepository;
    private final PromotionRecordRepository promotionRepo;

    @Transactional
    public StudentEnrollmentDto enroll(StudentEnrollmentRequest req) {
        // verify student & class
        studentRepository.findById(req.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        classRepository.findById(req.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));

        StudentEnrollment ent = enrollmentMapper.toEntity(req);
        ent.setEnrollmentDate(req.getEnrollmentDate() != null ? req.getEnrollmentDate() : LocalDate.now());
        StudentEnrollment saved = enrollmentRepository.save(ent);

        // update student's currentClass reference
        studentRepository.findById(req.getStudentId()).ifPresent(s -> {
            s.setCurrentClass(classRepository.findById(req.getClassId()).orElse(null));
            studentRepository.save(s);
        });

        return enrollmentMapper.toDto(saved);
    }

    @Transactional
    public PromotionRecord promote(PromotionRequest req) {
        studentRepository.findById(req.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        classRepository.findById(req.getToClassId())
                .orElseThrow(() -> new ResourceNotFoundException("To class not found"));

        PromotionRecord pr = new PromotionRecord();
        pr.setStudentId(req.getStudentId());
        pr.setFromClassId(req.getFromClassId());
        pr.setToClassId(req.getToClassId());
        pr.setSession(req.getSession());
        pr.setPromotedOn(req.getPromotedOn() != null ? req.getPromotedOn() : LocalDate.now());
        pr.setRemarks(req.getRemarks());
        pr.setFeePending(req.isFeePending());

        PromotionRecord saved = promotionRepo.save(pr);

        // update student's currentClass
        studentRepository.findById(req.getStudentId()).ifPresent(s -> {
            classRepository.findById(req.getToClassId()).ifPresent(c -> {
                s.setCurrentClass(c);
                studentRepository.save(s);
            });
        });

        return saved;
    }

    @Transactional(readOnly = true)
    public Page<StudentEnrollmentDto> listByClass(Long classId, Pageable pageable) {
        return enrollmentRepository.findByClassId(classId, pageable).map(enrollmentMapper::toDto);
    }
}
