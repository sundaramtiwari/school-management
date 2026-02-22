package com.school.backend.fee.controller;

import com.school.backend.fee.dto.FeeDiscountApplyRequest;
import com.school.backend.fee.dto.StudentFeeAssignRequest;
import com.school.backend.fee.dto.StudentFeeAssignmentDto;
import com.school.backend.fee.service.FeeDiscountService;
import com.school.backend.fee.service.StudentFeeAssignmentService;
import com.school.backend.user.security.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fees/assignments")
@RequiredArgsConstructor
public class StudentFeeAssignmentController {

    private final StudentFeeAssignmentService assignmentService;
    private final FeeDiscountService feeDiscountService;

    // Assign fee to student
    @PostMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'SUPER_ADMIN')")
    public StudentFeeAssignmentDto assign(
            @Valid @RequestBody StudentFeeAssignRequest req) {

        return assignmentService.assign(req);
    }

    // List student's assigned fees
    @GetMapping("/students/{studentId}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'ACCOUNTANT', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public List<StudentFeeAssignmentDto> listByStudent(
            @PathVariable Long studentId,
            @RequestParam Long sessionId) {

        return assignmentService.listByStudent(studentId, sessionId);
    }

    @PostMapping("/{assignmentId}/discount")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','ACCOUNTANT','SUPER_ADMIN','PLATFORM_ADMIN')")
    public StudentFeeAssignmentDto applyDiscount(
            @PathVariable Long assignmentId,
            @Valid @RequestBody FeeDiscountApplyRequest req) {

        return assignmentService.toDto(feeDiscountService.applyDiscount(
                assignmentId,
                req.getDiscountDefinitionId(),
                SecurityUtil.schoolId(),
                req.getRemarks(),
                SecurityUtil.userId()));
    }
}
