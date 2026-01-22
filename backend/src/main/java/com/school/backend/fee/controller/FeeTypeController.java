package com.school.backend.fee.controller;

import com.school.backend.fee.entity.FeeType;
import com.school.backend.fee.service.FeeTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fees/types")
@RequiredArgsConstructor
public class FeeTypeController {

    private final FeeTypeService service;

    // Create fee type
    @PostMapping
    public FeeType create(@RequestBody FeeType type) {
        return service.create(type);
    }

    // List fee types
    @GetMapping
    public List<FeeType> list() {
        return service.list();
    }

    // Get by id (optional but useful)
    @GetMapping("/{id}")
    public FeeType get(@PathVariable Long id) {
        return service.get(id);
    }
}
