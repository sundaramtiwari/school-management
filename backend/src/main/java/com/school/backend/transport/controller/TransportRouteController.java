package com.school.backend.transport.controller;

import com.school.backend.transport.dto.TransportRouteDto;
import com.school.backend.transport.service.TransportRouteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transport/routes")
@RequiredArgsConstructor
public class TransportRouteController {

    private final TransportRouteService routeService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PLATFORM_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<TransportRouteDto> create(@RequestBody TransportRouteDto dto) {
        return ResponseEntity.ok(routeService.createRoute(dto));
    }

    @GetMapping
    public ResponseEntity<List<TransportRouteDto>> getAll() {
        return ResponseEntity.ok(routeService.getAllRoutes());
    }

    // Add DELETE mapping for transport routes
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','SUPER_ADMIN','PLATFORM_ADMIN')")
    public ResponseEntity<Void> deleteRoute(@PathVariable Long id) {
        routeService.deleteRoute(id);
        return ResponseEntity.noContent().build();
    }
}
