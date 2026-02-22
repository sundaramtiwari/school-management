package com.school.backend.fee.service;

import com.school.backend.fee.dto.DiscountDefinitionDto;
import com.school.backend.fee.repository.DiscountDefinitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DiscountDefinitionService {

    private final DiscountDefinitionRepository discountDefinitionRepository;

    @Transactional(readOnly = true)
    public List<DiscountDefinitionDto> findActiveBySchool(Long schoolId) {
        return discountDefinitionRepository.findBySchoolIdAndActiveTrue(schoolId)
                .stream()
                .map(discount -> {
                    DiscountDefinitionDto dto = new DiscountDefinitionDto();
                    dto.setId(discount.getId());
                    dto.setName(discount.getName());
                    dto.setType(discount.getType());
                    dto.setAmountValue(discount.getAmountValue());
                    return dto;
                })
                .toList();
    }
}
