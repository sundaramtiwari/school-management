package com.school.backend.core.student.controller;

import com.school.backend.core.student.dto.PromotionRequest;
import com.school.backend.core.student.entity.PromotionRecord;
import com.school.backend.core.student.service.PromotionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/promotions")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionService promotionService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<List<PromotionRecord>> promoteStudents(@Valid @RequestBody PromotionRequest request) {
        return ResponseEntity.ok(promotionService.promoteStudents(request));
    }
}
