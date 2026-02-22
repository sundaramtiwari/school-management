package com.school.backend.fee.service;

import com.school.backend.common.enums.FundingCoverageMode;
import com.school.backend.common.exception.InvalidOperationException;
import com.school.backend.common.tenant.TenantContext;
import com.school.backend.fee.entity.StudentFundingArrangement;
import com.school.backend.fee.repository.StudentFundingArrangementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StudentFundingArrangementService {

    private final StudentFundingArrangementRepository fundingRepository;

    @Transactional
    public StudentFundingArrangement create(StudentFundingArrangement arrangement) {
        Long schoolId = TenantContext.getSchoolId();

        if (arrangement.getCoverageValue() == null ||
                arrangement.getCoverageValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidOperationException("Coverage value must be greater than 0");
        }

        if (arrangement.getCoverageMode() == FundingCoverageMode.PERCENTAGE &&
                arrangement.getCoverageValue().compareTo(new BigDecimal("100")) > 0) {
            throw new InvalidOperationException("Percentage coverage cannot exceed 100%");
        }

        if (arrangement.getValidFrom() != null && arrangement.getValidTo() != null &&
                arrangement.getValidFrom().isAfter(arrangement.getValidTo())) {
            throw new InvalidOperationException("validFrom must be before validTo");
        }

        long activeCount = fundingRepository.countByStudentIdAndSessionIdAndSchoolIdAndActiveTrue(
                arrangement.getStudentId(), arrangement.getSessionId(), schoolId);
        if (activeCount > 1) {
            throw new InvalidOperationException(
                    "Data integrity violation: multiple active funding arrangements found for student/session.");
        }
        if (activeCount == 1) {
            throw new InvalidOperationException(
                    "Student already has an active funding arrangement for this session. " +
                            "Please deactivate the existing one first.");
        }

        arrangement.setSchoolId(schoolId);
        arrangement.setActive(true);
        return fundingRepository.save(arrangement);
    }

    @Transactional(readOnly = true)
    public Optional<StudentFundingArrangement> getActive(Long studentId, Long sessionId) {
        return fundingRepository.findByStudentIdAndSessionIdAndSchoolIdAndActiveTrue(
                studentId, sessionId, TenantContext.getSchoolId());
    }

    @Transactional(readOnly = true)
    public List<StudentFundingArrangement> getAllForStudent(Long studentId) {
        return fundingRepository.findByStudentIdAndSchoolIdOrderByValidFromDescIdDesc(studentId, TenantContext.getSchoolId());
    }
}
