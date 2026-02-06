package com.school.backend.fee.service;

import com.school.backend.common.exception.ResourceNotFoundException;
import com.school.backend.common.tenant.TenantContext;
import com.school.backend.fee.dto.FeeStructureCreateRequest;
import com.school.backend.fee.dto.FeeStructureDto;
import com.school.backend.fee.entity.FeeStructure;
import com.school.backend.fee.entity.FeeType;
import com.school.backend.fee.repository.FeeStructureRepository;
import com.school.backend.fee.repository.FeeTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FeeStructureService {

    private final FeeStructureRepository feeStructureRepository;
    private final FeeTypeRepository feeTypeRepository;

    // ---------------- CREATE ----------------
    @Transactional
    public FeeStructureDto create(FeeStructureCreateRequest req) {

        FeeType feeType = feeTypeRepository.findById(req.getFeeTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("FeeType not found: " + req.getFeeTypeId()));

        FeeStructure fs = FeeStructure.builder()
                .schoolId(TenantContext.getSchoolId())
                .classId(req.getClassId())
                .session(req.getSession())
                .feeType(feeType)
                .amount(req.getAmount())
                .frequency(req.getFrequency() != null ? req.getFrequency()
                        : com.school.backend.fee.enums.FeeFrequency.ONE_TIME)
                .active(true)
                .build();

        return toDto(feeStructureRepository.save(fs));
    }

    // ---------------- LIST ----------------
    @Transactional(readOnly = true)
    public List<FeeStructureDto> listByClass(Long classId, String session) {

        return feeStructureRepository
                .findByClassIdAndSession(classId, session)
                .stream()
                .map(this::toDto)
                .toList();
    }

    // ---------------- MAPPER ----------------
    private FeeStructureDto toDto(FeeStructure fs) {

        FeeStructureDto dto = new FeeStructureDto();

        dto.setId(fs.getId());
        dto.setSchoolId(fs.getSchoolId());
        dto.setClassId(fs.getClassId());
        dto.setSession(fs.getSession());

        dto.setFeeTypeId(fs.getFeeType().getId());
        dto.setFeeTypeName(fs.getFeeType().getName());

        dto.setAmount(fs.getAmount());
        dto.setFrequency(fs.getFrequency());
        dto.setActive(fs.isActive());

        return dto;
    }
}
