package com.school.backend.fee.controller;

import com.school.backend.common.tenant.TenantContext;
import com.school.backend.fee.entity.StudentFundingArrangement;
import com.school.backend.fee.repository.StudentFundingArrangementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fees/funding")
@RequiredArgsConstructor
public class StudentFundingArrangementController {

    private final StudentFundingArrangementRepository fundingRepository;

    @PostMapping
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<StudentFundingArrangement> create(@RequestBody StudentFundingArrangement arrangement) {
        arrangement.setSchoolId(TenantContext.getSchoolId());
        arrangement.setActive(true);
        return ResponseEntity.ok(fundingRepository.save(arrangement));
    }

    @GetMapping("/student/{studentId}/session/{sessionId}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<StudentFundingArrangement> getActive(
            @PathVariable Long studentId,
            @PathVariable Long sessionId) {
        return fundingRepository.findActiveByStudentAndSession(studentId, sessionId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        fundingRepository.findById(id).ifPresent(f -> {
            f.setActive(false);
            fundingRepository.save(f);
        });
        return ResponseEntity.noContent().build();
    }
}
