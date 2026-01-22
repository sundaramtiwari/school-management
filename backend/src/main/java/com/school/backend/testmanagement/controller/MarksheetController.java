package com.school.backend.testmanagement.controller;

import com.school.backend.testmanagement.dto.MarksheetDto;
import com.school.backend.testmanagement.service.MarksheetService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/marksheets")
@RequiredArgsConstructor
public class MarksheetController {

    private final MarksheetService service;

    @GetMapping("/exam/{examId}/student/{studentId}")
    public MarksheetDto generate(
            @PathVariable Long examId,
            @PathVariable Long studentId) {

        return service.generate(examId, studentId);
    }
}
