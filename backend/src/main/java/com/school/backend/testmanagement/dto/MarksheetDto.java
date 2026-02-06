package com.school.backend.testmanagement.dto;

import lombok.Data;

import java.util.List;

@Data
public class MarksheetDto {

    private Long studentId;
    private Long examId;

    private Integer totalMarks;
    private Integer maxMarks;
    private Double percentage;

    private String grade;
    private boolean passed;

    private List<SubjectMark> subjects;

    @Data
    public static class SubjectMark {

        private Long subjectId;
        private String subjectName;
        private Integer marksObtained;
        private Integer maxMarks;
    }
}
