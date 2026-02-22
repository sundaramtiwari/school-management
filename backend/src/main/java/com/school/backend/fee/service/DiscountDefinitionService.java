package com.school.backend.fee.service;

import com.school.backend.common.enums.DiscountType;
import com.school.backend.common.exception.BusinessException;
import com.school.backend.common.exception.ResourceNotFoundException;
import com.school.backend.fee.dto.DiscountDefinitionCreateRequest;
import com.school.backend.fee.dto.DiscountDefinitionDto;
import com.school.backend.fee.entity.DiscountDefinition;
import com.school.backend.fee.repository.DiscountDefinitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DiscountDefinitionService {

    private final DiscountDefinitionRepository discountDefinitionRepository;

    @Transactional(readOnly = true)
    public List<DiscountDefinitionDto> findActiveBySchool(Long schoolId) {
        return discountDefinitionRepository.findBySchoolIdAndActiveTrue(schoolId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public DiscountDefinitionDto create(Long schoolId, DiscountDefinitionCreateRequest request) {
        if (request.getType() == DiscountType.PERCENTAGE
                && request.getAmountValue().compareTo(new BigDecimal("100")) > 0) {
            throw new BusinessException("amountValue must be <= 100 for percentage discounts");
        }

        boolean duplicateActive = discountDefinitionRepository.findBySchoolIdAndActiveTrue(schoolId)
                .stream()
                .anyMatch(def -> def.getName() != null
                        && def.getName().trim().equalsIgnoreCase(request.getName().trim()));
        if (duplicateActive) {
            throw new BusinessException("Discount definition with same name already exists.");
        }

        DiscountDefinition entity = DiscountDefinition.builder()
                .name(request.getName().trim())
                .type(request.getType())
                .amountValue(request.getAmountValue())
                .active(request.getActive() == null || request.getActive())
                .schoolId(schoolId)
                .build();

        return toDto(discountDefinitionRepository.save(entity));
    }

    @Transactional
    public DiscountDefinitionDto toggleActive(Long id, Long schoolId) {
        DiscountDefinition definition = discountDefinitionRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> {
                    if (discountDefinitionRepository.existsAnyById(id)) {
                        throw new AccessDeniedException("Access denied for discount definition: " + id);
                    }
                    return new ResourceNotFoundException("Discount definition not found: " + id);
                });

        definition.setActive(!definition.isActive());
        return toDto(discountDefinitionRepository.save(definition));
    }

    private DiscountDefinitionDto toDto(DiscountDefinition discount) {
        DiscountDefinitionDto dto = new DiscountDefinitionDto();
        dto.setId(discount.getId());
        dto.setName(discount.getName());
        dto.setType(discount.getType());
        dto.setAmountValue(discount.getAmountValue());
        dto.setActive(discount.isActive());
        return dto;
    }
}
