package com.school.backend.fee.service;

import com.school.backend.common.exception.ResourceNotFoundException;
import com.school.backend.core.student.entity.Student;
import com.school.backend.core.student.repository.StudentRepository;
import com.school.backend.fee.dto.FeeStatsDto;
import com.school.backend.fee.dto.FeeSummaryDto;
import com.school.backend.fee.entity.FeePayment;
import com.school.backend.fee.entity.FeeStructure;
import com.school.backend.fee.entity.StudentFeeAssignment;
import com.school.backend.fee.enums.FeeFrequency;
import com.school.backend.fee.repository.FeePaymentRepository;
import com.school.backend.fee.repository.FeeStructureRepository;
import com.school.backend.fee.repository.StudentFeeAssignmentRepository;
import com.school.backend.user.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FeeSummaryService {

        private final StudentRepository studentRepository;
        private final FeeStructureRepository feeStructureRepository;
        private final StudentFeeAssignmentRepository assignmentRepository;
        private final FeePaymentRepository paymentRepository;

        @Transactional(readOnly = true)
        public FeeStatsDto getDashboardStats(String session) {
                Long schoolId = SecurityUtil.schoolId();

                // 1. Today's Collection
                Long todayPaid = paymentRepository.sumAmountPaidBySchoolIdAndPaymentDate(schoolId, LocalDate.now());
                if (todayPaid == null)
                        todayPaid = 0L;

                // 2. Total Students
                long totalStudents = studentRepository.countBySchoolId(schoolId);

                // 3. Pending Dues (Institutional)
                // Naive implementation: iterate all students and sum their pending
                // In a production app, this would be a specialized aggregation table or a batch
                // job
                long totalPending = 0;
                // Get all students for the school
                // We use a simple list fetch for now, assuming institution size is manageable
                // (< 2000 students)
                List<Student> students = studentRepository
                                .findBySchoolId(schoolId, Pageable.unpaged())
                                .getContent();

                for (Student student : students) {
                        totalPending += getStudentFeeSummary(student.getId(), session).getPendingFee();
                }

                return FeeStatsDto.builder()
                                .todayCollection(todayPaid)
                                .totalStudents(totalStudents)
                                .pendingDues(totalPending)
                                .build();
        }

        @Transactional(readOnly = true)
        public FeeSummaryDto getStudentFeeSummary(Long studentId, String session) {

                if (!studentRepository.existsById(studentId)) {
                        throw new ResourceNotFoundException("Student not found: " + studentId);
                }

                // 1. Fetch assigned fee structures
                List<StudentFeeAssignment> assignments = assignmentRepository.findByStudentIdAndSession(studentId,
                                session);

                // 2. Calculate Total Accrued Fee (How much *should* have been paid by today)
                long totalFeeAccrued = 0;

                for (StudentFeeAssignment assignment : assignments) {
                        FeeStructure structure = feeStructureRepository.findById(assignment.getFeeStructureId())
                                        .orElse(null);

                        if (structure != null) {
                                int multiplier = calculateMultiplier(structure.getFrequency(), session);
                                totalFeeAccrued += (long) structure.getAmount() * multiplier;
                        }
                }

                // 3. Calculate total paid
                long totalPaid = paymentRepository.findByStudentId(studentId)
                                .stream()
                                .mapToLong(FeePayment::getAmountPaid)
                                .sum();

                // 4. Pending
                long pending = Math.max(totalFeeAccrued - totalPaid, 0);

                // 5. Build response
                FeeSummaryDto dto = new FeeSummaryDto();
                dto.setStudentId(studentId);

                Student s = studentRepository.findById(studentId).orElse(null);
                if (s != null) {
                        dto.setStudentName(s.getFirstName() + " " + (s.getLastName() != null ? s.getLastName() : ""));
                }

                dto.setSession(session);
                dto.setTotalFee((int) totalFeeAccrued);
                dto.setTotalPaid((int) totalPaid);
                dto.setPendingFee((int) pending);
                dto.setFeePending(pending > 0);

                return dto;
        }

        @Transactional(readOnly = true)
        public List<FeeSummaryDto> getTopDefaulters(String session, int limit) {
                Long schoolId = SecurityUtil.schoolId();
                List<Student> students = studentRepository
                                .findBySchoolId(schoolId, Pageable.unpaged())
                                .getContent();

                return students.stream()
                                .map(s -> getStudentFeeSummary(s.getId(), session))
                                .filter(dto -> dto.getPendingFee() > 0)
                                .sorted((a, b) -> Integer.compare(b.getPendingFee(), a.getPendingFee()))
                                .limit(limit)
                                .toList();
        }

        /**
         * Calculates how many times the fee is due based on current date.
         * Assumes Session starts in APRIL.
         */
        private int calculateMultiplier(FeeFrequency frequency, String session) {
                if (frequency == FeeFrequency.ONE_TIME || frequency == FeeFrequency.ANNUALLY) {
                        return 1;
                }

                // Parse session year "2025-26" -> 2025
                int startYear = Integer.parseInt(session.split("-")[0]);
                LocalDate sessionStart = LocalDate.of(startYear, Month.APRIL, 1);
                LocalDate now = LocalDate.now();

                if (now.isBefore(sessionStart)) {
                        return 0; // Session hasn't started
                }

                // Calculate months passed
                // e.g., April (start) -> April (now) = 1 month due
                // April -> May = 2 months due
                long monthsPassed = ChronoUnit.MONTHS.between(sessionStart, now) + 1;

                if (monthsPassed < 1)
                        monthsPassed = 1;
                if (monthsPassed > 12)
                        monthsPassed = 12; // Cap at 12 months

                if (frequency == FeeFrequency.MONTHLY) {
                        return (int) monthsPassed;
                }

                if (frequency == FeeFrequency.QUARTERLY) {
                        // 1-3 months = 1 quarter, 4-6 = 2, etc.
                        return (int) Math.ceil((double) monthsPassed / 3);
                }

                if (frequency == FeeFrequency.HALF_YEARLY) {
                        // 1-6 months = 1, 7-12 = 2
                        return (int) Math.ceil((double) monthsPassed / 6);
                }

                if (frequency == FeeFrequency.ANNUALLY) {
                        return 1;
                }

                return 1;
        }
}
