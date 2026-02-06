package com.school.backend.fee.controller;

import com.school.backend.fee.service.FeeChallanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.school.backend.user.security.SecurityUtil;

@RestController
@RequestMapping("/api/fees/challan")
@RequiredArgsConstructor
public class FeeChallanController {

        private final FeeChallanService challanService;

        /**
         * Download fee challan for a student
         * 
         * @param studentId Student ID
         * @param session   Academic session (e.g., "2024-25")
         * @return PDF file as byte array
         */
        @GetMapping("/student/{studentId}")
        @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'ACCOUNTANT', 'SUPER_ADMIN')")
        public ResponseEntity<byte[]> downloadChallan(
                        @PathVariable Long studentId,
                        @RequestParam String session) {

                byte[] pdf = challanService.generateChallan(studentId, session, SecurityUtil.schoolId());

                return ResponseEntity.ok()
                                .header(HttpHeaders.CONTENT_DISPOSITION,
                                                "attachment; filename=fee_challan_" + studentId + "_" + session
                                                                + ".pdf")
                                .contentType(MediaType.APPLICATION_PDF)
                                .body(pdf);
        }
}
