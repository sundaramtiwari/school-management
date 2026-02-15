package com.school.backend.testmanagement.service;

import com.school.backend.common.enums.ExamStatus;
import com.school.backend.common.exception.BusinessException;
import com.school.backend.user.security.SecurityUtil;
import com.school.backend.core.student.repository.StudentEnrollmentRepository;
import com.school.backend.testmanagement.dto.BulkMarksDto;
import com.school.backend.testmanagement.dto.ExamCreateRequest;
import com.school.backend.testmanagement.dto.ExamDto;
import com.school.backend.testmanagement.entity.Exam;
import com.school.backend.testmanagement.entity.ExamSubject;
import com.school.backend.testmanagement.entity.StudentMark;
import com.school.backend.testmanagement.mapper.ExamMapper;
import com.school.backend.testmanagement.repository.ExamRepository;
import com.school.backend.testmanagement.repository.ExamSubjectRepository;
import com.school.backend.testmanagement.repository.StudentMarkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class ExamService {

    private final ExamRepository repository;
    private final ExamSubjectRepository subjectRepository;
    private final StudentMarkRepository markRepository;
    private final StudentEnrollmentRepository studentEnrollmentRepository;
    private final ExamMapper examMapper;
    private final com.school.backend.school.service.SetupValidationService setupValidationService;

    @Transactional
    public ExamDto publishExam(Long examId) {
        Exam exam = repository.findById(examId)
                .orElseThrow(() -> new BusinessException("Exam not found: " + examId));

        // 1. Tenant Check
        if (!exam.getSchoolId().equals(SecurityUtil.schoolId())) {
            throw new BusinessException("Access denied to exam: " + examId);
        }

        // 2. Status Check
        if (exam.getStatus() == ExamStatus.PUBLISHED) {
            throw new BusinessException("Exam already published.");
        }
        if (exam.getStatus() == ExamStatus.LOCKED) {
            throw new BusinessException("Exam is locked and cannot be modified.");
        }
        if (exam.getStatus() != ExamStatus.DRAFT) {
            throw new BusinessException("Invalid status for publishing.");
        }

        // 3. Subject Count Check
        long subjectCount = subjectRepository.countByExamId(examId);
        if (subjectCount == 0) {
            throw new BusinessException("Cannot publish exam with 0 subjects.");
        }

        // 4. Student Count Check
        long studentCount = studentEnrollmentRepository.countByClassIdAndSessionIdAndActiveTrue(
                exam.getClassId(), exam.getSessionId());
        if (studentCount == 0) {
            throw new BusinessException("Cannot publish exam with 0 active students.");
        }

        // 5. Max Marks Check
        boolean invalidMaxMarks = subjectRepository.existsByExamIdAndMaxMarksLessThanEqual(examId, 0);
        if (invalidMaxMarks) {
            throw new BusinessException("All subjects must have max marks > 0.");
        }

        // 6. Marks Check
        long actualMarks = markRepository.countByExamId(examId);
        if (actualMarks == 0) {
            throw new BusinessException("Cannot publish exam without any marks entered.");
        }

        exam.setStatus(ExamStatus.PUBLISHED);
        exam = repository.save(exam);
        return examMapper.toDto(exam);
    }

    @Transactional
    public ExamDto lockExam(Long examId) {
        Exam exam = repository.findById(examId)
                .orElseThrow(() -> new BusinessException("Exam not found: " + examId));

        // 1. Tenant Check
        if (!exam.getSchoolId().equals(SecurityUtil.schoolId())) {
            throw new BusinessException("Access denied to exam: " + examId);
        }

        // 2. Status Check
        if (exam.getStatus() == ExamStatus.DRAFT) {
            throw new BusinessException("Publish exam before locking.");
        }
        if (exam.getStatus() == ExamStatus.LOCKED) {
            throw new BusinessException("Exam already locked.");
        }
        if (exam.getStatus() != ExamStatus.PUBLISHED) {
            throw new BusinessException("Invalid status for locking.");
        }

        exam.setStatus(ExamStatus.LOCKED);
        exam = repository.save(exam);
        return examMapper.toDto(exam);
    }

    // Create exam
    @Transactional
    public Exam create(ExamCreateRequest req) {
        setupValidationService.ensureAtLeastOneClassExists(req.getSchoolId(), req.getSessionId());

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
        Exam exam = repository.findById(examId)
                .orElseThrow(() -> new IllegalArgumentException("Exam not found"));
        setupValidationService.ensureAtLeastOneClassExists(exam.getSchoolId(), exam.getSessionId());

        if (exam.getStatus() != ExamStatus.DRAFT) {
            throw new BusinessException("Marks can only be entered when exam is in DRAFT status.");
        }

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
                            .examId(examId) // Ensure examId is populated
                            .examSubjectId(item.getExamSubjectId())
                            .studentId(item.getStudentId())
                            .build());

            mark.setMarksObtained(item.getMarksObtained());
            markRepository.save(mark);
        }
    }
}
