package com.school.backend.fee.controller;

import com.school.backend.fee.dto.*;
import com.school.backend.fee.service.FeeAdjustmentService;
import com.school.backend.fee.service.FeeDiscountService;
import com.school.backend.fee.service.LateFeeWaiverService;
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
    private final FeeAdjustmentService feeAdjustmentService;
    private final LateFeeWaiverService lateFeeWaiverService;

    // Assign fee to student
    @PostMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'SUPER_ADMIN', 'ACCOUNTANT')")
    public StudentFeeAssignmentDto assign(
            @Valid @RequestBody StudentFeeAssignRequest req) {

        return assignmentService.assign(req);
    }

    // List student's assigned fees
    @GetMapping("/students/{studentId}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'ACCOUNTANT', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public List<StudentFeeAssignmentDto> listByStudent(@PathVariable Long studentId) {
        return assignmentService.listByStudent(studentId);
    }

    @PostMapping("/{assignmentId}/discount")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','ACCOUNTANT','SUPER_ADMIN')")
    public FeeDiscountApplyResponse applyDiscount(
            @PathVariable Long assignmentId,
            @Valid @RequestBody FeeDiscountApplyRequest req) {

        return feeDiscountService.applyDiscount(
                assignmentId,
                req.getDiscountDefinitionId(),
                SecurityUtil.schoolId(),
                req.getRemarks(),
                SecurityUtil.userId());
    }

    @GetMapping("/{assignmentId}/adjustments")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','ACCOUNTANT','SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public List<FeeAdjustmentDto> getAdjustments(@PathVariable Long assignmentId) {
        return feeAdjustmentService.getAdjustmentsForAssignment(assignmentId, SecurityUtil.schoolId());
    }

    @PostMapping("/{assignmentId}/waive-late-fee")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','ACCOUNTANT','SUPER_ADMIN')")
    public StudentFeeAssignmentDto waiveLateFee(
            @PathVariable Long assignmentId,
            @Valid @RequestBody LateFeeWaiverRequest req) {

        return assignmentService.toDto(lateFeeWaiverService.waiveLateFee(
                assignmentId,
                req.getWaiverAmount(),
                req.getRemarks(),
                SecurityUtil.schoolId(),
                SecurityUtil.userId()));
    }
}
