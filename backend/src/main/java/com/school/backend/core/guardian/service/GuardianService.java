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

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GuardianService {

    private final GuardianRepository repository;
    private final GuardianMapper mapper;

    @Transactional
    public Guardian findOrCreateByContact(Long schoolId, GuardianCreateRequest req) {
        return repository.findBySchoolIdAndContactNumber(schoolId, req.getContactNumber())
                .map(existingGuardian -> repository.save(mergeGuardian(existingGuardian, req)))
                .orElseGet(() -> repository.save(buildGuardian(schoolId, req)));
    }

    private Guardian mergeGuardian(Guardian guardian, GuardianCreateRequest req) {
        if (req.getName() != null && !req.getName().isBlank()) {
            guardian.setName(req.getName());
        }
        if (req.getEmail() != null && !req.getEmail().isBlank()) {
            guardian.setEmail(req.getEmail());
        }
        if (req.getAddress() != null && !req.getAddress().isBlank()) {
            guardian.setAddress(req.getAddress());
        }
        if (req.getOccupation() != null && !req.getOccupation().isBlank()) {
            guardian.setOccupation(req.getOccupation());
        }
        if (req.getQualification() != null && !req.getQualification().isBlank()) {
            guardian.setQualification(req.getQualification());
        }
        if (req.getAadharNumber() != null && !req.getAadharNumber().isBlank()) {
            guardian.setAadharNumber(req.getAadharNumber());
        }
        if (req.getRelation() != null && !req.getRelation().isBlank()) {
            guardian.setRelation(req.getRelation());
        }
        guardian.setWhatsappEnabled(req.isWhatsappEnabled());
        return guardian;
    }

    private Guardian buildGuardian(Long schoolId, GuardianCreateRequest req) {
        Guardian guardian = Guardian.builder()
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
        guardian.setSchoolId(schoolId);
        return guardian;
    }


    @Transactional
    public Guardian findOrCreateByContactWithoutMutatingExisting(Long schoolId, GuardianCreateRequest req) {
        Optional<Guardian> existing = repository.findBySchoolIdAndContactNumber(schoolId, req.getContactNumber());
        if (existing.isPresent()) {
            return existing.get();
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
