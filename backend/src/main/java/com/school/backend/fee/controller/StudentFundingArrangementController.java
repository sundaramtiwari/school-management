package com.school.backend.fee.controller;

import com.school.backend.fee.entity.StudentFundingArrangement;
import com.school.backend.fee.service.StudentFundingArrangementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fees/funding")
@RequiredArgsConstructor
@Slf4j
public class StudentFundingArrangementController {

    private final StudentFundingArrangementService fundingService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<StudentFundingArrangement> create(@RequestBody StudentFundingArrangement arrangement) {
        log.info("Creating funding arrangement: studentId={}, sessionId={}, coverageType={}, coverageMode={}, value={}",
                arrangement.getStudentId(), arrangement.getSessionId(), arrangement.getCoverageType(),
                arrangement.getCoverageMode(), arrangement.getCoverageValue());
        StudentFundingArrangement saved = fundingService.create(arrangement);
        log.info("Funding arrangement created: id={}, studentId={}, sessionId={}",
                saved.getId(), saved.getStudentId(), saved.getSessionId());
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/student/{studentId}/session/{sessionId}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'ACCOUNTANT', 'SUPER_ADMIN')")
    public ResponseEntity<StudentFundingArrangement> getActive(
            @PathVariable Long studentId,
            @PathVariable Long sessionId) {
        log.debug("Fetching active funding arrangement from session context: studentId={}, requestedSessionId={}",
                studentId, sessionId);
        return fundingService.getActive(studentId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.ok().build());
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'ACCOUNTANT', 'SUPER_ADMIN')")
    public ResponseEntity<List<StudentFundingArrangement>> getAllForStudent(@PathVariable Long studentId) {
        log.debug("Fetching all funding arrangements for studentId={}", studentId);
        return ResponseEntity.ok(fundingService.getAllForStudent(studentId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("Deactivating funding arrangement: id={}", id);
        fundingService.deactivate(id);
        log.info("Funding arrangement deactivated (if existed): id={}", id);
        return ResponseEntity.noContent().build();
    }
}
