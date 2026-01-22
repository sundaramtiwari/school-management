package com.school.backend.testmanagement.controller;

import com.school.backend.testmanagement.dto.MarkEntryRequest;
import com.school.backend.testmanagement.entity.StudentMark;
import com.school.backend.testmanagement.service.MarkEntryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/marks")
@RequiredArgsConstructor
public class MarkEntryController {

    private final MarkEntryService service;

    @PostMapping
    public StudentMark enter(@RequestBody MarkEntryRequest req) {
        return service.enterMarks(req);
    }
}
