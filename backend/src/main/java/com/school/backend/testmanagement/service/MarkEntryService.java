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

        if (markRepository.existsByExamSubjectIdAndStudentId(
                req.getExamSubjectId(), req.getStudentId())) {

            throw new IllegalStateException("Marks already entered");
        }

        StudentMark mark = StudentMark.builder()
                .examSubjectId(req.getExamSubjectId())
                .studentId(req.getStudentId())
                .marksObtained(req.getMarksObtained())
                .remarks(req.getRemarks())
                .schoolId(TenantContext.getSchoolId())
                .build();

        return markRepository.save(mark);
    }
}
