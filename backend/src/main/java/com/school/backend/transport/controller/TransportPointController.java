package com.school.backend.transport.controller;

import com.school.backend.transport.dto.PickupPointDto;
import com.school.backend.transport.service.PickupPointService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transport/pickup-points")
@RequiredArgsConstructor
public class TransportPointController {

    private final PickupPointService pickupPointService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PLATFORM_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<PickupPointDto> create(@RequestBody PickupPointDto dto) {
        return ResponseEntity.ok(pickupPointService.createPickupPoint(dto));
    }

    @GetMapping("/route/{routeId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PLATFORM_ADMIN', 'SCHOOL_ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<List<PickupPointDto>> getByRoute(@PathVariable Long routeId) {
        return ResponseEntity.ok(pickupPointService.getByRoute(routeId));
    }
}
