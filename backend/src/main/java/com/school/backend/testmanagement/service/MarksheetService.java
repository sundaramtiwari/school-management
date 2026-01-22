package com.school.backend.testmanagement.service;

import com.school.backend.common.exception.ResourceNotFoundException;
import com.school.backend.testmanagement.dto.MarksheetDto;
import com.school.backend.testmanagement.entity.Exam;
import com.school.backend.testmanagement.entity.GradePolicy;
import com.school.backend.testmanagement.repository.ExamRepository;
import com.school.backend.testmanagement.repository.GradePolicyRepository;
import com.school.backend.testmanagement.repository.MarksheetQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MarksheetService {

    private final MarksheetQueryRepository queryRepo;
    private final ExamRepository examRepository;

    private final GradePolicyRepository gradeRepo;


    @Transactional(readOnly = true)
    public MarksheetDto generate(Long examId, Long studentId) {

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam not found"));

        List<Object[]> rows =
                queryRepo.fetchStudentMarks(examId, studentId);

        if (rows.isEmpty()) {
            throw new ResourceNotFoundException("No subjects found for exam");
        }

        int total = 0;
        int max = 0;

        List<MarksheetDto.SubjectMark> subjects = new ArrayList<>();

        for (Object[] r : rows) {

            Long subjectId = (Long) r[0];
            Integer maxMarks = (Integer) r[1];
            Integer obtained = (Integer) r[2];

            if (obtained == null) {
                obtained = 0;
            }

            total += obtained;
            max += maxMarks;

            MarksheetDto.SubjectMark sm =
                    new MarksheetDto.SubjectMark();

            sm.setSubjectId(subjectId);
            sm.setMaxMarks(maxMarks);
            sm.setMarksObtained(obtained);

            subjects.add(sm);
        }

        double percent = max == 0 ? 0 : (total * 100.0) / max;

        MarksheetDto dto = new MarksheetDto();

        dto.setStudentId(studentId);
        dto.setExamId(examId);

        dto.setTotalMarks(total);
        dto.setMaxMarks(max);

        dto.setPercentage(round(percent));

        dto.setPassed(percent >= 33.0);

        dto.setGrade(resolveGrade(exam.getSchoolId(), percent));

        dto.setSubjects(subjects);

        return dto;
    }

    private String resolveGrade(Long schoolId, double percent) {

        List<GradePolicy> policies =
                gradeRepo.findBySchoolIdOrderByMinPercentDesc(schoolId);

        for (GradePolicy gp : policies) {

            if (percent >= gp.getMinPercent()
                    && percent <= gp.getMaxPercent()) {

                return gp.getGrade();
            }
        }

        return calcGrade(percent); // fallback
    }

    private String calcGrade(double p) {

        if (p >= 90) return "A+";
        if (p >= 80) return "A";
        if (p >= 70) return "B+";
        if (p >= 60) return "B";
        if (p >= 50) return "C";
        if (p >= 33) return "D";

        return "F";
    }

    private double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
