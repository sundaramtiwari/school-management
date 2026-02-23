package com.school.backend.fee.service;

import com.school.backend.common.exception.ResourceNotFoundException;
import com.school.backend.common.tenant.TenantContext;
import com.school.backend.fee.dto.FeeTypePatchRequest;
import com.school.backend.fee.entity.FeeType;
import com.school.backend.fee.repository.FeeTypeRepository;
import com.school.backend.user.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FeeTypeService {
    private static final Logger log = LoggerFactory.getLogger(FeeTypeService.class);

    private final FeeTypeRepository repository;

    // -------- Create --------
    @Transactional
    public FeeType create(FeeType type) {
        Long schoolId = TenantContext.getSchoolId();

        if (repository.existsByNameIgnoreCaseAndSchoolId(type.getName(), schoolId)) {
            throw new IllegalStateException("FeeType already exists: " + type.getName());
        }

        type.setActive(true);
        type.setSchoolId(schoolId);

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
        return repository.findByIdAndSchoolId(id, TenantContext.getSchoolId())
                .orElseThrow(() -> new ResourceNotFoundException("FeeType not found: " + id));
    }

    @Transactional
    public FeeType update(Long id, Long schoolId, FeeTypePatchRequest req) {
        FeeType feeType = repository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> {
                    if (repository.existsAnyById(id)) {
                        throw new AccessDeniedException("Access denied for fee type: " + id);
                    }
                    return new ResourceNotFoundException("FeeType not found: " + id);
                });

        if (req.getSchoolId() != null && !req.getSchoolId().equals(feeType.getSchoolId())) {
            throw new IllegalArgumentException("schoolId is immutable");
        }
        String oldName = feeType.getName();
        if (req.getName() != null && !req.getName().trim().isBlank()) {
            String nextName = req.getName().trim();
            if (!nextName.equalsIgnoreCase(feeType.getName()) &&
                    repository.existsByNameIgnoreCaseAndSchoolId(nextName, schoolId)) {
                throw new IllegalStateException("FeeType already exists: " + nextName);
            }
            feeType.setName(nextName);
        }
        FeeType saved = repository.save(feeType);
        log.info("event=fee_type_updated schoolId={} feeTypeId={} actorId={} oldName={} newName={} timestamp={}",
                schoolId,
                saved.getId(),
                SecurityUtil.userId(),
                oldName,
                saved.getName(),
                Instant.now());
        return saved;
    }

    @Transactional
    public FeeType toggleActive(Long id, Long schoolId) {
        FeeType feeType = repository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> {
                    if (repository.existsAnyById(id)) {
                        throw new AccessDeniedException("Access denied for fee type: " + id);
                    }
                    return new ResourceNotFoundException("FeeType not found: " + id);
                });

        boolean oldActive = feeType.isActive();
        feeType.setActive(!oldActive);
        FeeType saved = repository.save(feeType);
        log.info("event=fee_type_toggled schoolId={} feeTypeId={} actorId={} oldActive={} newActive={} timestamp={}",
                schoolId,
                saved.getId(),
                SecurityUtil.userId(),
                oldActive,
                saved.isActive(),
                Instant.now());
        return saved;
    }
}
