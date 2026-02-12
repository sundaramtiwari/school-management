package com.school.backend.fee.controller;

import com.school.backend.fee.dto.StudentFeeAssignRequest;
import com.school.backend.fee.dto.StudentFeeAssignmentDto;
import com.school.backend.fee.service.StudentFeeAssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fees/assignments")
@RequiredArgsConstructor
public class StudentFeeAssignmentController {

    private final StudentFeeAssignmentService service;

    // Assign fee to student
    @PostMapping
    public StudentFeeAssignmentDto assign(
            @Valid @RequestBody StudentFeeAssignRequest req) {

        return service.assign(req);
    }

    // List student's assigned fees
    @GetMapping("/students/{studentId}")
    public List<StudentFeeAssignmentDto> listByStudent(
            @PathVariable Long studentId,
            @RequestParam Long sessionId) {

        return service.listByStudent(studentId, sessionId);
    }
}
