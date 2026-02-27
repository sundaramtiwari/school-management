package com.school.backend.fee.controller;

import com.school.backend.common.tenant.SessionContext;
import com.school.backend.fee.service.FeeChallanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fees/challan")
@RequiredArgsConstructor
public class FeeChallanController {

    private final FeeChallanService challanService;

    /**
     * Download fee challan for a student
     *
     * @param studentId Student ID
     * @return PDF file as byte array
     */
    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'ACCOUNTANT', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<byte[]> downloadChallan(
            @PathVariable Long studentId) {

        Long sessionId = SessionContext.getSessionId();
        byte[] pdf = challanService.generateChallan(studentId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=fee_challan_" + studentId + "_" + sessionId
                                + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
