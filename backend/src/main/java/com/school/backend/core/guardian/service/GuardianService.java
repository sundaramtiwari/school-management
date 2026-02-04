package com.school.backend.core.guardian.service;

import com.school.backend.common.exception.ResourceNotFoundException;
import com.school.backend.common.tenant.TenantContext;
import com.school.backend.core.guardian.dto.GuardianCreateRequest;
import com.school.backend.core.guardian.dto.GuardianDto;
import com.school.backend.core.guardian.entity.Guardian;
import com.school.backend.core.guardian.mapper.GuardianMapper;
import com.school.backend.core.guardian.repository.GuardianRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GuardianService {

    private final GuardianRepository repository;
    private final GuardianMapper mapper;

    @Transactional
    public GuardianDto create(GuardianCreateRequest req) {
        Guardian g = mapper.toEntity(req);
        g.setSchoolId(TenantContext.getSchoolId());
        return mapper.toDto(repository.save(g));
    }

    @Transactional(readOnly = true)
    public Page<GuardianDto> listBySchool(Long schoolId, Pageable pageable) {
        return repository.findBySchoolId(schoolId, pageable).map(mapper::toDto);
    }

    @Transactional(readOnly = true)
    public GuardianDto getById(Long id) {
        return repository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Guardian not found: " + id));
    }

    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        return repository.existsById(id);
    }

}
