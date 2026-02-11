package com.school.backend.testmanagement.service;

import com.school.backend.common.exception.ResourceNotFoundException;
import com.school.backend.common.tenant.TenantContext;
import com.school.backend.testmanagement.dto.MarkEntryRequest;
import com.school.backend.testmanagement.entity.ExamSubject;
import com.school.backend.testmanagement.entity.StudentMark;
import com.school.backend.testmanagement.repository.ExamSubjectRepository;
import com.school.backend.testmanagement.repository.StudentMarkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MarkEntryService {

    private final StudentMarkRepository markRepository;
    private final ExamSubjectRepository subjectRepository;

    @Transactional
    public StudentMark enterMarks(MarkEntryRequest req) {

        ExamSubject subject = subjectRepository.findById(req.getExamSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("ExamSubject not found"));

        if (req.getMarksObtained() > subject.getMaxMarks()) {
            throw new IllegalArgumentException("Marks exceed max marks");
        }

        StudentMark mark = markRepository.findByExamSubjectIdAndStudentId(
                        req.getExamSubjectId(), req.getStudentId())
                .orElseGet(() -> StudentMark.builder()
                        .examSubjectId(req.getExamSubjectId())
                        .studentId(req.getStudentId())
                        .schoolId(TenantContext.getSchoolId()) // Ensure schoolId is set for new entries
                        .build());

        mark.setMarksObtained(req.getMarksObtained());
        mark.setRemarks(req.getRemarks());

        return markRepository.save(mark);
    }

    @Transactional(readOnly = true)
    public List<StudentMark> getMarksByExam(Long examId) {
        List<Long> subjectIds = subjectRepository.findByExamId(examId)
                .stream()
                .map(ExamSubject::getId)
                .toList();

        return markRepository.findByExamSubjectIdIn(subjectIds);
    }
}
