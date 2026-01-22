package com.school.backend.fee.controller;

import com.school.backend.fee.dto.FeeStructureCreateRequest;
import com.school.backend.fee.dto.FeeStructureDto;
import com.school.backend.fee.service.FeeStructureService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fees/structures")
@RequiredArgsConstructor
public class FeeStructureController {

    private final FeeStructureService service;

    // Create fee structure
    @PostMapping
    public FeeStructureDto create(@Valid @RequestBody FeeStructureCreateRequest req) {
        return service.create(req);
    }

    // List by class+session
    @GetMapping("/by-class/{classId}")
    public List<FeeStructureDto> listByClass(
            @PathVariable Long classId,
            @RequestParam String session) {

        return service.listByClass(classId, session);
    }
}
