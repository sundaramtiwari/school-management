package com.school.backend.testmanagement.service;

import com.school.backend.common.exception.ResourceNotFoundException;
import com.school.backend.common.tenant.TenantContext;
import com.school.backend.testmanagement.dto.MarkEntryRequest;
import com.school.backend.testmanagement.entity.Exam;
import com.school.backend.testmanagement.entity.ExamSubject;
import com.school.backend.testmanagement.entity.StudentMark;
import com.school.backend.testmanagement.repository.ExamRepository;
import com.school.backend.testmanagement.repository.ExamSubjectRepository;
import com.school.backend.testmanagement.repository.StudentMarkRepository;
import com.school.backend.core.teacher.entity.Teacher;
import com.school.backend.core.teacher.repository.TeacherRepository;
import com.school.backend.core.classsubject.repository.ClassSubjectRepository;
import com.school.backend.user.security.SecurityUtil;
import com.school.backend.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MarkEntryService {

        private final StudentMarkRepository markRepository;
        private final ExamSubjectRepository subjectRepository;
        private final ExamRepository examRepository;
        private final TeacherRepository teacherRepository;
        private final ClassSubjectRepository classSubjectRepository;
        private final com.school.backend.school.service.SetupValidationService setupValidationService;

        @Transactional
        public StudentMark enterMarks(MarkEntryRequest req) {
                Long schoolId = TenantContext.getSchoolId();

                ExamSubject subject = subjectRepository.findById(req.getExamSubjectId())
                                .orElseThrow(() -> new ResourceNotFoundException("ExamSubject not found"));
                Exam exam = examRepository.findById(subject.getExamId())
                                .orElseThrow(() -> new BusinessException("Exam not found"));
                setupValidationService.ensureAtLeastOneClassExists(schoolId, exam.getSessionId());

                if (SecurityUtil.hasRole("TEACHER")) {
                        Long userId = SecurityUtil.current().getUserId();
                        Teacher teacher = teacherRepository.findByUserId(userId)
                                        .orElseThrow(() -> new BusinessException("Teacher record not found for user"));

                        boolean assigned = classSubjectRepository
                                        .existsBySchoolClassSessionIdAndTeacherIdAndSchoolClassIdAndSubjectIdAndSchoolId(
                                                        exam.getSessionId(), teacher.getId(), exam.getClassId(),
                                                        subject.getSubjectId(), schoolId);

                        if (!assigned) {
                                throw new BusinessException(
                                                "Access Denied: You are not assigned to this class and subject in the current session.");
                        }
                }

                if (req.getMarksObtained() > subject.getMaxMarks()) {
                        throw new IllegalArgumentException("Marks exceed max marks");
                }

                StudentMark mark = markRepository.findByExamSubjectIdAndStudentId(
                                req.getExamSubjectId(), req.getStudentId())
                                .orElseGet(() -> StudentMark.builder()
                                                .examId(subject.getExamId()) // Set examId from ExamSubject
                                                .examSubjectId(req.getExamSubjectId())
                                                .studentId(req.getStudentId())
                                                .schoolId(TenantContext.getSchoolId()) // Ensure schoolId is set for new
                                                                                       // entries
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
