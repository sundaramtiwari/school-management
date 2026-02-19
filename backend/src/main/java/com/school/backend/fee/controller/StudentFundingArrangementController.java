package com.school.backend.fee.controller;

import com.school.backend.common.exception.InvalidOperationException;
import com.school.backend.common.tenant.TenantContext;
import com.school.backend.fee.entity.StudentFundingArrangement;
import com.school.backend.fee.enums.FundingCoverageMode;
import com.school.backend.fee.repository.StudentFundingArrangementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Optional;

@RestController
@RequestMapping("/api/fees/funding")
@RequiredArgsConstructor
public class StudentFundingArrangementController {

    private final StudentFundingArrangementRepository fundingRepository;

    @PostMapping
    @PreAuthorize("hasRole('SCHOOL_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<StudentFundingArrangement> create(@RequestBody StudentFundingArrangement arrangement) {
        // Validate
        if (arrangement.getCoverageValue() == null ||
                arrangement.getCoverageValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidOperationException("Coverage value must be greater than 0");
        }

        if (arrangement.getCoverageMode() == FundingCoverageMode.PERCENTAGE &&
                arrangement.getCoverageValue().compareTo(new BigDecimal("100")) > 0) {
            throw new InvalidOperationException("Percentage coverage cannot exceed 100%");
        }

        if (arrangement.getValidFrom() != null && arrangement.getValidTo() != null &&
                arrangement.getValidFrom().isAfter(arrangement.getValidTo())) {
            throw new InvalidOperationException("validFrom must be before validTo");
        }

        // Check for existing active funding
        Optional<StudentFundingArrangement> existing = fundingRepository
                .findActiveByStudentAndSession(arrangement.getStudentId(), arrangement.getSessionId());

        if (existing.isPresent()) {
            throw new InvalidOperationException(
                    "Student already has an active funding arrangement for this session. " +
                            "Please deactivate the existing one first.");
        }

        arrangement.setSchoolId(TenantContext.getSchoolId());
        arrangement.setActive(true);
        return ResponseEntity.ok(fundingRepository.save(arrangement));
    }

    @GetMapping("/student/{studentId}/session/{sessionId}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'ACCOUNTANT', 'SUPER_ADMIN')")
    public ResponseEntity<StudentFundingArrangement> getActive(
            @PathVariable Long studentId,
            @PathVariable Long sessionId) {
        return fundingRepository.findActiveByStudentAndSession(studentId, sessionId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SCHOOL_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        fundingRepository.findById(id).ifPresent(f -> {
            f.setActive(false);
            fundingRepository.save(f);
        });
        return ResponseEntity.noContent().build();
    }
}
