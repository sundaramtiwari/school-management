package com.school.backend.testmanagement.service;

import com.school.backend.testmanagement.dto.BulkMarksDto;
import com.school.backend.testmanagement.dto.ExamCreateRequest;
import com.school.backend.testmanagement.entity.Exam;
import com.school.backend.testmanagement.entity.ExamSubject;
import com.school.backend.testmanagement.entity.StudentMark;
import com.school.backend.testmanagement.repository.ExamRepository;
import com.school.backend.testmanagement.repository.ExamSubjectRepository;
import com.school.backend.testmanagement.repository.StudentMarkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamService {

    private final ExamRepository repository;
    private final ExamSubjectRepository subjectRepository;
    private final StudentMarkRepository markRepository;

    // Create exam
    @Transactional
    public Exam create(ExamCreateRequest req) {

        Exam exam = Exam.builder()
                .schoolId(req.getSchoolId())
                .classId(req.getClassId())
                .sessionId(req.getSessionId())
                .name(req.getName())
                .examType(req.getExamType())
                .active(true)
                .build();

        return repository.save(exam);
    }

    // List exams
    @Transactional(readOnly = true)
    public List<Exam> listByClass(Long classId, Long sessionId) {

        return repository.findByClassIdAndSessionId(classId, sessionId);
    }

    // Bulk save marks
    @Transactional
    public void saveMarksBulk(Long examId, BulkMarksDto dto) {

        // 1. Get all subjects for this exam for validation
        List<ExamSubject> subjects = subjectRepository.findByExamId(examId);
        Map<Long, ExamSubject> subjectMap = subjects.stream()
                .collect(Collectors.toMap(ExamSubject::getId, Function.identity()));

        // 2. Process and save marks
        for (BulkMarksDto.MarkItem item : dto.getMarks()) {
            ExamSubject subject = subjectMap.get(item.getExamSubjectId());
            if (subject == null) {
                throw new IllegalArgumentException("Invalid ExamSubjectId for this exam: " + item.getExamSubjectId());
            }

            if (item.getMarksObtained() > subject.getMaxMarks()) {
                throw new IllegalArgumentException("Marks exceed maximum marks (" +
                        subject.getMaxMarks() + ") for subject: " + item.getExamSubjectId());
            }

            // Upsert (Find existing or create new)
            StudentMark mark = markRepository.findByExamSubjectIdAndStudentId(
                    item.getExamSubjectId(), item.getStudentId())
                    .orElseGet(() -> StudentMark.builder()
                            .examSubjectId(item.getExamSubjectId())
                            .studentId(item.getStudentId())
                            .build());

            mark.setMarksObtained(item.getMarksObtained());
            markRepository.save(mark);
        }
    }
}
