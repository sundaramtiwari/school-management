package com.school.backend.core.guardian.mapper;

import com.school.backend.core.guardian.dto.GuardianCreateRequest;
import com.school.backend.core.guardian.dto.GuardianDto;
import com.school.backend.core.guardian.entity.Guardian;

public interface GuardianMapper {
    GuardianDto toDto(Guardian entity);

    Guardian toEntity(GuardianCreateRequest dto);
}
