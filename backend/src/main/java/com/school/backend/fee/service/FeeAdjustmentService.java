package com.school.backend.fee.service;

import com.school.backend.common.exception.ResourceNotFoundException;
import com.school.backend.fee.dto.FeeAdjustmentDto;
import com.school.backend.fee.entity.FeeAdjustment;
import com.school.backend.fee.entity.StudentFeeAssignment;
import com.school.backend.fee.repository.FeeAdjustmentRepository;
import com.school.backend.fee.repository.StudentFeeAssignmentRepository;
import com.school.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeeAdjustmentService {

    private static final String LEGACY_DISCOUNT = "Legacy Discount";
    private static final String SYSTEM = "System";

    private final StudentFeeAssignmentRepository assignmentRepository;
    private final FeeAdjustmentRepository adjustmentRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<FeeAdjustmentDto> getAdjustmentsForAssignment(Long assignmentId, Long schoolId) {
        StudentFeeAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student fee assignment not found: " + assignmentId));

        if (!Objects.equals(assignment.getSchoolId(), schoolId)) {
            throw new AccessDeniedException("Access denied for assignment: " + assignmentId);
        }

        List<FeeAdjustment> adjustments = adjustmentRepository.findByAssignmentIdOrderByCreatedAtAsc(assignmentId);

        Map<Long, String> usersById = userRepository.findAllById(adjustments.stream()
                .map(FeeAdjustment::getCreatedByStaff)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .filter(s -> s.chars().allMatch(Character::isDigit))
                .map(Long::valueOf)
                .toList()).stream()
                .collect(Collectors.toMap(
                        user -> user.getId(),
                        user -> (user.getFullName() == null || user.getFullName().isBlank()) ? SYSTEM
                                : user.getFullName().trim()));

        return adjustments.stream()
                .map(adjustment -> toDto(adjustment, usersById))
                .toList();
    }

    private FeeAdjustmentDto toDto(
            FeeAdjustment adjustment,
            Map<Long, String> usersById) {

        FeeAdjustmentDto dto = new FeeAdjustmentDto();
        dto.setId(adjustment.getId());
        dto.setAssignmentId(adjustment.getAssignmentId());
        dto.setType(adjustment.getType());
        dto.setAmount(adjustment.getAmount());
        dto.setRemarks(adjustment.getReason());
        dto.setCreatedAt(adjustment.getCreatedAt());
        dto.setCreatedByName(resolveCreator(adjustment.getCreatedByStaff(), usersById));
        dto.setDiscountName(resolveDiscountName(adjustment));
        return dto;
    }

    private String resolveCreator(String createdByStaff, Map<Long, String> usersById) {
        if (createdByStaff == null || createdByStaff.isBlank()) {
            return SYSTEM;
        }
        try {
            return usersById.getOrDefault(Long.valueOf(createdByStaff.trim()), SYSTEM);
        } catch (NumberFormatException ex) {
            return SYSTEM;
        }
    }

    private String resolveDiscountName(FeeAdjustment adjustment) {
        if (adjustment.getType() != FeeAdjustment.AdjustmentType.DISCOUNT) {
            return null;
        }
        if (adjustment.getDiscountNameSnapshot() != null && !adjustment.getDiscountNameSnapshot().isBlank()) {
            return adjustment.getDiscountNameSnapshot().trim();
        }
        return LEGACY_DISCOUNT;
    }
}
