package com.school.backend.core.guardian.service;

import com.school.backend.common.exception.ResourceNotFoundException;
import com.school.backend.common.tenant.TenantContext;
import com.school.backend.core.guardian.dto.GuardianCreateRequest;
import com.school.backend.core.guardian.dto.GuardianDto;
import com.school.backend.core.guardian.entity.Guardian;
import com.school.backend.core.guardian.mapper.GuardianMapper;
import com.school.backend.core.guardian.repository.GuardianRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GuardianService {

    private final GuardianRepository repository;
    private final GuardianMapper mapper;

    private static @NonNull Guardian getGuardian(GuardianCreateRequest req, Optional<Guardian> existing) {
        Guardian g = existing.get();
        // Update details when request has non-blank values.
        if (req.getName() != null && !req.getName().isBlank())
            g.setName(req.getName());
        if (req.getEmail() != null && !req.getEmail().isBlank())
            g.setEmail(req.getEmail());
        if (req.getAddress() != null && !req.getAddress().isBlank())
            g.setAddress(req.getAddress());
        if (req.getOccupation() != null && !req.getOccupation().isBlank())
            g.setOccupation(req.getOccupation());
        if (req.getQualification() != null && !req.getQualification().isBlank())
            g.setQualification(req.getQualification());
        if (req.getAadharNumber() != null && !req.getAadharNumber().isBlank())
            g.setAadharNumber(req.getAadharNumber());
        if (req.getRelation() != null && !req.getRelation().isBlank())
            g.setRelation(req.getRelation());

        g.setWhatsappEnabled(req.isWhatsappEnabled());
        return g;
    }

    @Transactional
    public Guardian findOrCreateByContact(Long schoolId, GuardianCreateRequest req) {
        Optional<Guardian> existing = repository.findBySchoolIdAndContactNumber(schoolId, req.getContactNumber());

        if (existing.isPresent()) {
            Guardian g = getGuardian(req, existing);

            return repository.save(g);
        }

        Guardian g = Guardian.builder()
                .name(req.getName())
                .contactNumber(req.getContactNumber())
                .relation(req.getRelation())
                .email(req.getEmail())
                .address(req.getAddress())
                .aadharNumber(req.getAadharNumber())
                .occupation(req.getOccupation())
                .qualification(req.getQualification())
                .whatsappEnabled(req.isWhatsappEnabled())
                .active(true)
                .build();
        g.setSchoolId(schoolId);
        return repository.save(g);
    }

    @Transactional
    public GuardianDto create(GuardianCreateRequest req) {
        return mapper.toDto(findOrCreateByContact(TenantContext.getSchoolId(), req));
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
    public Optional<Guardian> getOptionalById(Long id) {
        return repository.findById(id);
    }

    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        return repository.existsById(id);
    }
}
