package com.school.backend.testmanagement.controller;

import com.school.backend.testmanagement.dto.GradePolicyRequest;
import com.school.backend.testmanagement.entity.GradePolicy;
import com.school.backend.testmanagement.service.GradePolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.school.backend.user.security.SecurityUtil;

import java.util.List;

@RestController
@RequestMapping("/api/grades")
@RequiredArgsConstructor
public class GradePolicyController {

    private final GradePolicyService service;

    @PostMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'SUPER_ADMIN')")
    public GradePolicy create(@RequestBody GradePolicyRequest req) {
        req.setSchoolId(SecurityUtil.schoolId());
        return service.create(req);
    }

    @GetMapping("/mine")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'SUPER_ADMIN')")
    public List<GradePolicy> list() {
        return service.getForSchool(SecurityUtil.schoolId());
    }
}
