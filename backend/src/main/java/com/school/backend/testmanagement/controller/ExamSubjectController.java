package com.school.backend.testmanagement.controller;

import com.school.backend.testmanagement.dto.ExamSubjectCreateRequest;
import com.school.backend.testmanagement.entity.ExamSubject;
import com.school.backend.testmanagement.service.ExamSubjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/exam-subjects")
@RequiredArgsConstructor
public class ExamSubjectController {

    private final ExamSubjectService service;

    @PostMapping
    public ExamSubject create(@RequestBody ExamSubjectCreateRequest req) {
        return service.create(req);
    }
}
