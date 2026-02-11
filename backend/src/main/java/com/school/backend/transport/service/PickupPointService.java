package com.school.backend.transport.service;

import com.school.backend.common.tenant.TenantContext;
import com.school.backend.common.exception.ResourceNotFoundException;
import com.school.backend.transport.dto.PickupPointDto;
import com.school.backend.transport.entity.PickupPoint;
import com.school.backend.transport.entity.TransportRoute;
import com.school.backend.transport.repository.PickupPointRepository;
import com.school.backend.transport.repository.TransportRouteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PickupPointService {

    private final PickupPointRepository pickupPointRepository;
    private final TransportRouteRepository routeRepository;

    @Transactional
    public PickupPointDto createPickupPoint(PickupPointDto dto) {
        TransportRoute route = routeRepository.findById(dto.getRouteId())
                .orElseThrow(() -> new ResourceNotFoundException("Route not found: " + dto.getRouteId()));

        PickupPoint pp = PickupPoint.builder()
                .name(dto.getName())
                .amount(dto.getAmount())
                .frequency(dto.getFrequency())
                .route(route)
                .schoolId(TenantContext.getSchoolId())
                .build();

        pp = pickupPointRepository.save(pp);
        return mapToDto(pp);
    }

    @Transactional(readOnly = true)
    public List<PickupPointDto> getByRoute(Long routeId) {
        return pickupPointRepository.findByRouteId(routeId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private PickupPointDto mapToDto(PickupPoint pp) {
        return PickupPointDto.builder()
                .id(pp.getId())
                .name(pp.getName())
                .amount(pp.getAmount())
                .frequency(pp.getFrequency())
                .routeId(pp.getRoute().getId())
                .build();
    }
}
