package com.school.backend.testmanagement.mapper;

import com.school.backend.testmanagement.dto.ExamDto;
import com.school.backend.testmanagement.entity.Exam;
import org.springframework.stereotype.Component;

@Component
public class ExamMapper {

    public ExamDto toDto(Exam exam) {
        if (exam == null) {
            return null;
        }

        return ExamDto.builder()
                .id(exam.getId())
                .schoolId(exam.getSchoolId())
                .classId(exam.getClassId())
                .sessionId(exam.getSessionId())
                .name(exam.getName())
                .examType(exam.getExamType())
                .startDate(exam.getStartDate())
                .endDate(exam.getEndDate())
                .active(exam.isActive())
                .status(exam.getStatus())
                .build();
    }
}
