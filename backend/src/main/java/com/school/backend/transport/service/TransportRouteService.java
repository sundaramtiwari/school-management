package com.school.backend.transport.service;

import com.school.backend.common.tenant.TenantContext;
import com.school.backend.common.exception.ResourceNotFoundException;
import com.school.backend.transport.dto.TransportRouteDto;
import com.school.backend.transport.entity.TransportRoute;
import com.school.backend.transport.repository.TransportRouteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransportRouteService {

    private final TransportRouteRepository routeRepository;

    @Transactional
    public TransportRouteDto createRoute(TransportRouteDto dto) {
        TransportRoute route = TransportRoute.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .schoolId(TenantContext.getSchoolId())
                .build();
        route = routeRepository.save(route);
        return mapToDto(route);
    }

    @Transactional(readOnly = true)
    public List<TransportRouteDto> getAllRoutes() {
        return routeRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private TransportRouteDto mapToDto(TransportRoute route) {
        return TransportRouteDto.builder()
                .id(route.getId())
                .name(route.getName())
                .description(route.getDescription())
                .build();
    }
}
