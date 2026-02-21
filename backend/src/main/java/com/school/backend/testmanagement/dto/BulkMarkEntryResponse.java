package com.school.backend.testmanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkMarkEntryResponse {
    private int savedCount;
    private int skippedCount;
    private List<SkippedSubject> skippedSubjects;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SkippedSubject {
        private Long subjectId;
        private String subjectName;
        private String reason;
    }
}
