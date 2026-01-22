package com.school.backend.testmanagement.controller;

import com.school.backend.testmanagement.dto.GradePolicyRequest;
import com.school.backend.testmanagement.entity.GradePolicy;
import com.school.backend.testmanagement.service.GradePolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/grades")
@RequiredArgsConstructor
public class GradePolicyController {

    private final GradePolicyService service;

    @PostMapping
    public GradePolicy create(@RequestBody GradePolicyRequest req) {
        return service.create(req);
    }

    @GetMapping("/school/{schoolId}")
    public List<GradePolicy> list(@PathVariable Long schoolId) {
        return service.getForSchool(schoolId);
    }
}
