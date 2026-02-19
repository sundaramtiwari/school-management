package com.school.backend.fee.service;

import com.school.backend.common.exception.ResourceNotFoundException;
import com.school.backend.core.student.entity.Student;
import com.school.backend.core.student.repository.StudentEnrollmentRepository;
import com.school.backend.core.student.repository.StudentRepository;
import com.school.backend.fee.dto.DefaulterDto;
import com.school.backend.fee.dto.FeeStatsDto;
import com.school.backend.fee.dto.FeeSummaryDto;
import com.school.backend.fee.entity.StudentFeeAssignment;
import com.school.backend.fee.repository.FeePaymentRepository;
import com.school.backend.fee.repository.StudentFeeAssignmentRepository;
import com.school.backend.school.entity.AcademicSession;
import com.school.backend.school.repository.AcademicSessionRepository;
import com.school.backend.school.repository.SchoolRepository;
import com.school.backend.user.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FeeSummaryService {

        private final StudentRepository studentRepository;
        private final StudentEnrollmentRepository enrollmentRepository;
        private final StudentFeeAssignmentRepository assignmentRepository;
        private final FeePaymentRepository paymentRepository;
        private final AcademicSessionRepository sessionRepository;
        private final SchoolRepository schoolRepository;
        private final LateFeeCalculator lateFeeCalculator;

        // ---------------------------------------------------
        // DASHBOARD STATS
        // ---------------------------------------------------
        @Transactional(readOnly = true)
        public FeeStatsDto getDashboardStats(Long sessionId) {

                Long schoolId = SecurityUtil.schoolId();

                if (!sessionRepository.existsByIdAndSchoolId(sessionId, schoolId)) {
                        throw new ResourceNotFoundException("Session not found: " + sessionId);
                }

                // 1. Today's Collection
                java.math.BigDecimal todayPaid = paymentRepository
                                .sumTotalPaidBySchoolIdAndPaymentDate(schoolId, LocalDate.now());

                java.math.BigDecimal todayCollection = todayPaid != null ? todayPaid : java.math.BigDecimal.ZERO;

                // 2. Total Students (SESSION AWARE)
                long totalStudents = enrollmentRepository
                                .countBySchoolIdAndSessionIdAndActiveTrue(schoolId, sessionId);

                // 3. Optimized Pending Calculation (NO N+1)
                java.math.BigDecimal totalAssigned = assignmentRepository
                                .sumTotalAssignedBySchoolAndSession(schoolId, sessionId);

                java.math.BigDecimal totalPaid = paymentRepository
                                .sumTotalPaidBySchoolAndSession(schoolId, sessionId);

                java.math.BigDecimal totalPending = (totalAssigned != null ? totalAssigned : java.math.BigDecimal.ZERO)
                                .subtract(totalPaid != null ? totalPaid : java.math.BigDecimal.ZERO);

                if (totalPending.compareTo(java.math.BigDecimal.ZERO) < 0) {
                        totalPending = java.math.BigDecimal.ZERO;
                }

                return FeeStatsDto.builder()
                                .todayCollection(todayCollection)
                                .totalStudents(totalStudents)
                                .pendingDues(totalPending)
                                .build();
        }

        // ---------------------------------------------------
        // STUDENT SUMMARY
        // ---------------------------------------------------
        @Transactional(readOnly = true)
        public FeeSummaryDto getStudentFeeSummary(Long studentId, Long sessionId) {

                Long schoolId = SecurityUtil.schoolId();

                Student student = studentRepository
                                .findByIdAndSchoolId(studentId, schoolId)
                                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentId));

                AcademicSession session = sessionRepository.findById(sessionId)
                                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + sessionId));

                List<StudentFeeAssignment> assignments = assignmentRepository.findByStudentIdAndSessionId(studentId,
                                sessionId);

                java.math.BigDecimal totalFeeAccrued = assignments.stream()
                                .map(a -> nz(a.getAmount()))
                                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

                java.math.BigDecimal totalLateFeeAccrued = assignments.stream()
                                .map(sfa -> {
                                        java.math.BigDecimal unpaid = nz(sfa.getAmount())
                                                        .subtract(nz(sfa.getPrincipalPaid()))
                                                        .subtract(nz(sfa.getTotalDiscountAmount()))
                                                        .subtract(nz(sfa.getSponsorCoveredAmount()));
                                        java.math.BigDecimal incremental = lateFeeCalculator.calculateLateFee(sfa,
                                                        unpaid, LocalDate.now());
                                        return nz(sfa.getLateFeeAccrued()).add(incremental);
                                })
                                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

                java.math.BigDecimal totalLateFeePaid = assignments.stream()
                                .map(a -> nz(a.getLateFeePaid()))
                                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

                java.math.BigDecimal totalPrincipalPaid = assignments.stream()
                                .map(a -> nz(a.getPrincipalPaid()))
                                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

                java.math.BigDecimal totalPaid = totalPrincipalPaid.add(totalLateFeePaid);

                java.math.BigDecimal pending = totalFeeAccrued.add(totalLateFeeAccrued)
                                .subtract(totalPaid);

                if (pending.compareTo(java.math.BigDecimal.ZERO) < 0) {
                        pending = java.math.BigDecimal.ZERO;
                }

                FeeSummaryDto dto = new FeeSummaryDto();
                dto.setStudentId(studentId);
                dto.setStudentName(student.getFirstName() + " " +
                                (student.getLastName() != null ? student.getLastName() : ""));
                dto.setSession(session.getName());
                dto.setTotalFee(totalFeeAccrued.setScale(2, java.math.RoundingMode.HALF_UP));
                dto.setTotalPaid(totalPaid.setScale(2, java.math.RoundingMode.HALF_UP));
                dto.setTotalLateFeeAccrued(totalLateFeeAccrued.setScale(2, java.math.RoundingMode.HALF_UP));
                dto.setTotalLateFeePaid(totalLateFeePaid.setScale(2, java.math.RoundingMode.HALF_UP));
                dto.setPendingFee(pending.setScale(2, java.math.RoundingMode.HALF_UP));
                dto.setFeePending(pending.compareTo(java.math.BigDecimal.ZERO) > 0);

                return dto;
        }

        private java.math.BigDecimal nz(java.math.BigDecimal value) {
                return value != null ? value : java.math.BigDecimal.ZERO;
        }

        // ---------------------------------------------------
        // TOP DEFAULTERS
        // ---------------------------------------------------
        @Transactional(readOnly = true)
        public List<FeeSummaryDto> getTopDefaulters(Long sessionId, int limit) {

                Long schoolId = SecurityUtil.schoolId();

                if (!sessionRepository.existsByIdAndSchoolId(sessionId, schoolId)) {
                        throw new ResourceNotFoundException("Session not found: " + sessionId);
                }

                // 1. Load students (session aware)
                List<Student> students = studentRepository
                                .findBySchoolIdAndSessionId(schoolId, sessionId, Pageable.unpaged())
                                .getContent();

                if (students.isEmpty())
                        return List.of();

                // 2. Aggregated Assigned
                var assignedRaw = assignmentRepository
                                .sumAssignedGroupedByStudent(schoolId, sessionId);

                var assignedMap = assignedRaw.stream()
                                .collect(java.util.stream.Collectors.toMap(
                                                r -> (Long) r[0],
                                                r -> (java.math.BigDecimal) r[1]));

                // 3. Aggregated Paid
                var paidRaw = paymentRepository
                                .sumPaidGroupedByStudent(schoolId, sessionId);

                var paidMap = paidRaw.stream()
                                .collect(java.util.stream.Collectors.toMap(
                                                r -> (Long) r[0],
                                                r -> (java.math.BigDecimal) r[1]));

                String sessionName = sessionRepository.findById(sessionId)
                                .map(AcademicSession::getName)
                                .orElse("");

                List<FeeSummaryDto> results = new ArrayList<>();

                for (Student student : students) {

                        Long studentId = student.getId();

                        java.math.BigDecimal totalAssigned = assignedMap.getOrDefault(studentId,
                                        java.math.BigDecimal.ZERO);
                        java.math.BigDecimal totalPaid = paidMap.getOrDefault(studentId, java.math.BigDecimal.ZERO);

                        java.math.BigDecimal pending = totalAssigned.subtract(totalPaid);
                        if (pending.compareTo(java.math.BigDecimal.ZERO) < 0) {
                                pending = java.math.BigDecimal.ZERO;
                        }

                        if (pending.compareTo(java.math.BigDecimal.ZERO) <= 0)
                                continue;

                        FeeSummaryDto dto = new FeeSummaryDto();
                        dto.setStudentId(studentId);
                        dto.setStudentName(student.getFirstName() + " " +
                                        (student.getLastName() != null ? student.getLastName() : ""));
                        dto.setSession(sessionName);
                        dto.setTotalFee(totalAssigned);
                        dto.setTotalPaid(totalPaid);
                        dto.setPendingFee(pending);
                        dto.setFeePending(true);

                        results.add(dto);
                }

                return results.stream()
                                .sorted((a, b) -> b.getPendingFee().compareTo(a.getPendingFee()))
                                .limit(limit)
                                .toList();
        }

        // ---------------------------------------------------
        // ALL DEFAULTERS
        // ---------------------------------------------------
        @Transactional(readOnly = true)
        public List<DefaulterDto> getAllDefaulters() {

                Long schoolId = SecurityUtil.schoolId();

                com.school.backend.school.entity.School school = schoolRepository.findById(schoolId)
                                .orElseThrow(() -> new ResourceNotFoundException("School not found"));

                if (school.getCurrentSessionId() == null) {
                        return List.of();
                }

                Long sessionId = school.getCurrentSessionId();

                AcademicSession session = sessionRepository.findById(sessionId)
                                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));

                LocalDate sessionStart;
                try {
                        int startYear = Integer.parseInt(session.getName().split("-")[0]);
                        sessionStart = LocalDate.of(startYear, Month.APRIL, 1);
                } catch (Exception e) {
                        sessionStart = LocalDate.now().minusMonths(6);
                }

                LocalDate today = LocalDate.now();

                // 1. Load students (session aware)
                List<Student> students = studentRepository
                                .findBySchoolIdAndSessionId(schoolId, sessionId, Pageable.unpaged())
                                .getContent();

                if (students.isEmpty())
                        return List.of();

                // 2. Aggregated Assigned Amounts
                var assignedRaw = assignmentRepository
                                .sumAssignedGroupedByStudent(schoolId, sessionId);

                var assignedMap = assignedRaw.stream()
                                .collect(java.util.stream.Collectors.toMap(
                                                r -> (Long) r[0],
                                                r -> (java.math.BigDecimal) r[1]));

                var lateFeeAccruedMap = assignedRaw.stream()
                                .collect(java.util.stream.Collectors.toMap(
                                                r -> (Long) r[0],
                                                r -> (java.math.BigDecimal) r[2]));

                // 3. Aggregated Paid Amounts
                var paidRaw = paymentRepository
                                .sumPaidGroupedByStudent(schoolId, sessionId);

                var paidMap = paidRaw.stream()
                                .collect(java.util.stream.Collectors.toMap(
                                                r -> (Long) r[0],
                                                r -> (java.math.BigDecimal) r[1]));

                // 4. Last Payment Dates
                var lastPaymentRaw = paymentRepository
                                .findLastPaymentDateGroupedByStudent(schoolId, sessionId);

                var lastPaymentMap = lastPaymentRaw.stream()
                                .collect(java.util.stream.Collectors.toMap(
                                                r -> (Long) r[0],
                                                r -> (LocalDate) r[1]));

                List<DefaulterDto> defaulters = new ArrayList<>();

                for (Student student : students) {

                        Long studentId = student.getId();

                        java.math.BigDecimal totalAssigned = assignedMap.getOrDefault(studentId,
                                        java.math.BigDecimal.ZERO);
                        java.math.BigDecimal totalLateFeeAccrued = lateFeeAccruedMap.getOrDefault(studentId,
                                        java.math.BigDecimal.ZERO);
                        java.math.BigDecimal totalPaid = paidMap.getOrDefault(studentId, java.math.BigDecimal.ZERO);

                        java.math.BigDecimal pending = totalAssigned.add(totalLateFeeAccrued).subtract(totalPaid);
                        if (pending.compareTo(java.math.BigDecimal.ZERO) < 0) {
                                pending = java.math.BigDecimal.ZERO;
                        }

                        if (pending.compareTo(java.math.BigDecimal.ZERO) <= 0)
                                continue;

                        LocalDate lastPaymentDate = lastPaymentMap.get(studentId);

                        LocalDate overdueFrom = lastPaymentDate != null
                                        ? lastPaymentDate
                                        : sessionStart;

                        long daysOverdue = Math.max(
                                        ChronoUnit.DAYS.between(overdueFrom, today), 0);

                        String className = "";
                        String classSection = "";

                        if (student.getCurrentClass() != null) {
                                className = student.getCurrentClass().getName();
                                classSection = student.getCurrentClass().getSection();
                        }

                        defaulters.add(DefaulterDto.builder()
                                        .studentId(studentId)
                                        .studentName(student.getFirstName() + " " +
                                                        (student.getLastName() != null ? student.getLastName() : ""))
                                        .admissionNumber(student.getAdmissionNumber())
                                        .className(className)
                                        .classSection(classSection != null ? classSection : "")
                                        .amountDue(pending)
                                        .lateFeeAccrued(totalLateFeeAccrued)
                                        .lastPaymentDate(lastPaymentDate)
                                        .daysOverdue(daysOverdue)
                                        .parentContact(student.getContactNumber() != null
                                                        ? student.getContactNumber()
                                                        : "")
                                        .build());
                }

                defaulters.sort((a, b) -> b.getAmountDue().compareTo(a.getAmountDue()));

                return defaulters;
        }

}
