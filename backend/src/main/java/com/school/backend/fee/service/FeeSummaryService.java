package com.school.backend.fee.service;

import com.school.backend.common.exception.ResourceNotFoundException;
import com.school.backend.core.student.entity.Student;
import com.school.backend.core.student.repository.StudentEnrollmentRepository;
import com.school.backend.core.student.repository.StudentRepository;
import com.school.backend.fee.dto.DefaulterDto;
import com.school.backend.fee.dto.FeeStatsDto;
import com.school.backend.fee.dto.FeeSummaryDto;
import com.school.backend.fee.entity.FeePayment;
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
import java.util.Comparator;
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
        Long todayPaid = paymentRepository
                .sumAmountPaidBySchoolIdAndPaymentDate(schoolId, LocalDate.now());

        long todayCollection = todayPaid != null ? todayPaid : 0L;

        // 2. Total Students (SESSION AWARE)
        long totalStudents = enrollmentRepository
                .countBySchoolIdAndSessionIdAndActiveTrue(schoolId, sessionId);

        // 3. Optimized Pending Calculation (NO N+1)
        Long totalAssigned = assignmentRepository
                .sumTotalAssignedBySchoolAndSession(schoolId, sessionId);

        Long totalPaid = paymentRepository
                .sumTotalPaidBySchoolAndSession(schoolId, sessionId);

        long totalPending = (totalAssigned != null ? totalAssigned : 0L)
                - (totalPaid != null ? totalPaid : 0L);

        if (totalPending < 0) {
            totalPending = 0;
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
                .orElseThrow(() ->
                        new ResourceNotFoundException("Student not found: " + studentId));

        AcademicSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Session not found: " + sessionId));

        List<StudentFeeAssignment> assignments =
                assignmentRepository.findByStudentIdAndSessionId(studentId, sessionId);

        long totalFeeAccrued = assignments.stream()
                .mapToLong(StudentFeeAssignment::getAmount)
                .sum();

        long totalPaid = paymentRepository.findByStudentId(studentId)
                .stream()
                .filter(p -> sessionId.equals(p.getSessionId()))
                .mapToLong(FeePayment::getAmountPaid)
                .sum();

        long pending = Math.max(totalFeeAccrued - totalPaid, 0);

        FeeSummaryDto dto = new FeeSummaryDto();
        dto.setStudentId(studentId);
        dto.setStudentName(student.getFirstName() + " " +
                (student.getLastName() != null ? student.getLastName() : ""));
        dto.setSession(session.getName());
        dto.setTotalFee((int) totalFeeAccrued);
        dto.setTotalPaid((int) totalPaid);
        dto.setPendingFee((int) pending);
        dto.setFeePending(pending > 0);

        return dto;
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

        if (students.isEmpty()) return List.of();

        // 2. Aggregated Assigned
        var assignedRaw = assignmentRepository
                .sumAssignedGroupedByStudent(schoolId, sessionId);

        var assignedMap = assignedRaw.stream()
                .collect(java.util.stream.Collectors.toMap(
                        r -> (Long) r[0],
                        r -> (Long) r[1]
                ));

        // 3. Aggregated Paid
        var paidRaw = paymentRepository
                .sumPaidGroupedByStudent(schoolId, sessionId);

        var paidMap = paidRaw.stream()
                .collect(java.util.stream.Collectors.toMap(
                        r -> (Long) r[0],
                        r -> (Long) r[1]
                ));

        String sessionName = sessionRepository.findById(sessionId)
                .map(AcademicSession::getName)
                .orElse("");

        List<FeeSummaryDto> results = new ArrayList<>();

        for (Student student : students) {

            Long studentId = student.getId();

            long totalAssigned = assignedMap.getOrDefault(studentId, 0L);
            long totalPaid = paidMap.getOrDefault(studentId, 0L);

            long pending = Math.max(totalAssigned - totalPaid, 0);

            if (pending <= 0) continue;

            FeeSummaryDto dto = new FeeSummaryDto();
            dto.setStudentId(studentId);
            dto.setStudentName(student.getFirstName() + " " +
                    (student.getLastName() != null ? student.getLastName() : ""));
            dto.setSession(sessionName);
            dto.setTotalFee((int) totalAssigned);
            dto.setTotalPaid((int) totalPaid);
            dto.setPendingFee((int) pending);
            dto.setFeePending(true);

            results.add(dto);
        }

        return results.stream()
                .sorted((a, b) -> Integer.compare(b.getPendingFee(), a.getPendingFee()))
                .limit(limit)
                .toList();
    }


    // ---------------------------------------------------
    // ALL DEFAULTERS
    // ---------------------------------------------------
    @Transactional(readOnly = true)
    public List<DefaulterDto> getAllDefaulters() {

        Long schoolId = SecurityUtil.schoolId();

        com.school.backend.school.entity.School school =
                schoolRepository.findById(schoolId)
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

        if (students.isEmpty()) return List.of();

        // 2. Aggregated Assigned Amounts
        var assignedRaw = assignmentRepository
                .sumAssignedGroupedByStudent(schoolId, sessionId);

        var assignedMap = assignedRaw.stream()
                .collect(java.util.stream.Collectors.toMap(
                        r -> (Long) r[0],
                        r -> (Long) r[1]
                ));

        // 3. Aggregated Paid Amounts
        var paidRaw = paymentRepository
                .sumPaidGroupedByStudent(schoolId, sessionId);

        var paidMap = paidRaw.stream()
                .collect(java.util.stream.Collectors.toMap(
                        r -> (Long) r[0],
                        r -> (Long) r[1]
                ));

        // 4. Last Payment Dates
        var lastPaymentRaw = paymentRepository
                .findLastPaymentDateGroupedByStudent(schoolId, sessionId);

        var lastPaymentMap = lastPaymentRaw.stream()
                .collect(java.util.stream.Collectors.toMap(
                        r -> (Long) r[0],
                        r -> (LocalDate) r[1]
                ));

        List<DefaulterDto> defaulters = new ArrayList<>();

        for (Student student : students) {

            Long studentId = student.getId();

            long totalAssigned = assignedMap.getOrDefault(studentId, 0L);
            long totalPaid = paidMap.getOrDefault(studentId, 0L);

            long pending = Math.max(totalAssigned - totalPaid, 0);

            if (pending <= 0) continue;

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
                    .lastPaymentDate(lastPaymentDate)
                    .daysOverdue(daysOverdue)
                    .parentContact(student.getContactNumber() != null
                            ? student.getContactNumber()
                            : "")
                    .build());
        }

        defaulters.sort(Comparator.comparingLong(DefaulterDto::getAmountDue).reversed());

        return defaulters;
    }

}
