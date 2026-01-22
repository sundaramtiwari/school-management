package com.school.backend.testmanagement.service;

import com.school.backend.testmanagement.dto.ExamSubjectCreateRequest;
import com.school.backend.testmanagement.entity.ExamSubject;
import com.school.backend.testmanagement.repository.ExamSubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExamSubjectService {

    private final ExamSubjectRepository repository;

    @Transactional
    public ExamSubject create(ExamSubjectCreateRequest req) {

        if (repository.existsByExamIdAndSubjectId(
                req.getExamId(), req.getSubjectId())) {

            throw new IllegalStateException("Subject already added to exam");
        }

        ExamSubject es = ExamSubject.builder()
                .examId(req.getExamId())
                .subjectId(req.getSubjectId())
                .maxMarks(req.getMaxMarks())
                .active(true)
                .build();

        return repository.save(es);
    }
}
