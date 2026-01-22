package com.school.backend.testmanagement.service;

import com.school.backend.testmanagement.dto.ExamCreateRequest;
import com.school.backend.testmanagement.entity.Exam;
import com.school.backend.testmanagement.repository.ExamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExamService {

    private final ExamRepository repository;

    // Create exam
    @Transactional
    public Exam create(ExamCreateRequest req) {

        Exam exam = Exam.builder()
                .schoolId(req.getSchoolId())
                .classId(req.getClassId())
                .session(req.getSession())
                .name(req.getName())
                .examType(req.getExamType())
                .active(true)
                .build();

        return repository.save(exam);
    }

    // List exams
    @Transactional(readOnly = true)
    public List<Exam> listByClass(Long classId, String session) {

        return repository.findByClassIdAndSession(classId, session);
    }
}
