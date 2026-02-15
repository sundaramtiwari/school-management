package com.school.backend.testmanagement.service;

import com.school.backend.common.tenant.TenantContext;
import com.school.backend.testmanagement.dto.ExamSubjectCreateRequest;
import com.school.backend.testmanagement.entity.ExamSubject;
import com.school.backend.testmanagement.repository.ExamSubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExamSubjectService {

    private final ExamSubjectRepository repository;
    private final com.school.backend.testmanagement.repository.ExamRepository examRepository;

    @Transactional
    public ExamSubject create(ExamSubjectCreateRequest req) {
        com.school.backend.testmanagement.entity.Exam exam = examRepository.findById(req.getExamId())
                .orElseThrow(() -> new com.school.backend.common.exception.ResourceNotFoundException("Exam not found"));

        if (exam.getStatus() != com.school.backend.common.enums.ExamStatus.DRAFT) {
            throw new com.school.backend.common.exception.BusinessException(
                    "Cannot add subjects to a published or locked exam.");
        }

        if (repository.existsByExamIdAndSubjectId(
                req.getExamId(), req.getSubjectId())) {

            throw new IllegalStateException("Subject already added to exam");
        }

        ExamSubject es = ExamSubject.builder()
                .examId(req.getExamId())
                .subjectId(req.getSubjectId())
                .maxMarks(req.getMaxMarks())
                .schoolId(TenantContext.getSchoolId())
                .active(true)
                .build();

        return repository.save(es);
    }

    @Transactional(readOnly = true)
    public List<ExamSubject> listByExam(Long examId) {
        return repository.findByExamId(examId);
    }
}
