package com.school.backend.fee.controller;

import com.school.backend.fee.dto.FeeSummaryDto;
import com.school.backend.fee.service.FeeSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fees/summary")
@RequiredArgsConstructor
public class FeeSummaryController {

    private final FeeSummaryService service;

    @GetMapping("/students/{studentId}")
    public FeeSummaryDto getStudentFeeSummary(
            @PathVariable Long studentId,
            @RequestParam String session) {

        return service.getStudentFeeSummary(studentId, session);
    }
}
