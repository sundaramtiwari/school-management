package com.school.backend.fee.service;

import com.school.backend.common.exception.ResourceNotFoundException;
import com.school.backend.common.tenant.TenantContext;
import com.school.backend.fee.entity.FeeType;
import com.school.backend.fee.repository.FeeTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FeeTypeService {

    private final FeeTypeRepository repository;

    // -------- Create --------
    @Transactional
    public FeeType create(FeeType type) {

        if (repository.existsByNameIgnoreCase(type.getName())) {
            throw new IllegalStateException("FeeType already exists: " + type.getName());
        }

        type.setActive(true);
        type.setSchoolId(TenantContext.getSchoolId());

        return repository.save(type);
    }

    // -------- List --------
    @Transactional(readOnly = true)
    public List<FeeType> list() {
        return repository.findAll();
    }

    // -------- Get --------
    @Transactional(readOnly = true)
    public FeeType get(Long id) {
        return repository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("FeeType not found: " + id));
    }
}
