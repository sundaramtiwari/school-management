package com.school.backend.fee.service;

import com.school.backend.common.enums.FeeFrequency;
import com.school.backend.core.student.entity.StudentEnrollment;
import com.school.backend.core.student.repository.StudentEnrollmentRepository;
import com.school.backend.fee.entity.FeeStructure;
import com.school.backend.school.entity.AcademicSession;
import com.school.backend.school.repository.AcademicSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import static java.math.BigDecimal.ZERO;

@Service
@Slf4j
@RequiredArgsConstructor
public class FeeCalculationService {

    private static final int SCALE_2 = 2;
    private static final RoundingMode ROUNDING_HALF_UP = RoundingMode.HALF_UP;
    private static final BigDecimal ZERO_HALF_UP = ZERO.setScale(SCALE_2, ROUNDING_HALF_UP);
    private final AcademicSessionRepository academicSessionRepository;
    private final StudentEnrollmentRepository enrollmentRepository;

    public BigDecimal calculateNetPrincipal(BigDecimal amount, BigDecimal discount) {
        log.debug("Calculating net principal: amount={}, discount={}", amount, discount);
        BigDecimal net = amount.subtract(discount);
        BigDecimal calculated = net.compareTo(ZERO) < 0 ? ZERO_HALF_UP
                : net.setScale(SCALE_2, ROUNDING_HALF_UP);
        log.debug("Net principal calculated: {}", calculated);
        return calculated;
    }

    public BigDecimal calculateAssignableAmount(FeeStructure fs, Long studentId) {
        if (fs.getFrequency() == null || fs.getFrequency() == FeeFrequency.ONE_TIME
                || fs.getFrequency() == FeeFrequency.ANNUALLY) {
            return fs.getAmount();
        }

        AcademicSession session = academicSessionRepository.findById(fs.getSessionId())
                .filter(s -> fs.getSchoolId().equals(s.getSchoolId()))
                .orElse(null);

        // Fallback: If session or dates are missing, charge full-year periods
        if (session == null || session.getStartDate() == null || session.getEndDate() == null) {
            return fs.getAmount().multiply(BigDecimal.valueOf(fs.getFrequency().getPeriodsPerYear()));
        }

        LocalDate effectiveStart = enrollmentRepository
                .findFirstByStudentIdAndSessionIdAndActiveTrue(studentId, fs.getSessionId())
                .map(StudentEnrollment::getEnrollmentDate)
                .filter(d -> d.isAfter(session.getStartDate()))
                .orElse(session.getStartDate());

        long monthsRemaining = ChronoUnit.MONTHS.between(
                effectiveStart.withDayOfMonth(1),
                session.getEndDate().plusDays(1));

        if (monthsRemaining <= 0) {
            monthsRemaining = 1;
        }

        return switch (fs.getFrequency()) {
            case MONTHLY -> fs.getAmount().multiply(BigDecimal.valueOf(monthsRemaining));
            case QUARTERLY -> fs.getAmount().multiply(BigDecimal.valueOf(Math.ceil(monthsRemaining / 3.0)));
            case HALF_YEARLY -> fs.getAmount().multiply(BigDecimal.valueOf(Math.ceil(monthsRemaining / 6.0)));
            default -> fs.getAmount();
        };
    }
}
