package com.school.backend.student.controller;

import com.school.backend.student.dto.LedgerSummaryDto;
import com.school.backend.student.service.StudentLedgerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentLedgerController {

    private final StudentLedgerService studentLedgerService;

    @GetMapping("/{id}/ledger")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'ACCOUNTANT', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<List<LedgerSummaryDto>> getLedger(@PathVariable Long id) {
        return ResponseEntity.ok(studentLedgerService.getStudentLedger(id));
    }

    @GetMapping("/{id}/ledger-summary")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'ACCOUNTANT', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<List<LedgerSummaryDto>> getLedgerSummary(@PathVariable Long id) {
        return ResponseEntity.ok(studentLedgerService.getStudentLedger(id));
    }
}
