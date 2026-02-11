package com.school.backend.testmanagement.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class BulkMarksDto {

    @NotNull
    private List<MarkItem> marks;

    @Data
    public static class MarkItem {

        @NotNull
        private Long studentId;

        @NotNull
        private Long examSubjectId;

        @NotNull
        private Integer marksObtained;
    }
}
