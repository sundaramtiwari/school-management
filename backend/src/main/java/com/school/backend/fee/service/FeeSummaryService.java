package com.school.backend.fee.service;

import com.school.backend.common.exception.ResourceNotFoundException;
import com.school.backend.core.student.entity.Student;
import com.school.backend.core.student.repository.StudentRepository;
import com.school.backend.fee.dto.DefaulterDto;
import com.school.backend.fee.dto.FeeStatsDto;
import com.school.backend.fee.dto.FeeSummaryDto;
import com.school.backend.fee.entity.FeePayment;
import com.school.backend.fee.entity.StudentFeeAssignment;
import com.school.backend.fee.enums.FeeFrequency;
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
    private final StudentFeeAssignmentRepository assignmentRepository;
    private final FeePaymentRepository paymentRepository;
    private final AcademicSessionRepository sessionRepository;
    private final SchoolRepository schoolRepository;

    @Transactional(readOnly = true)
    public FeeStatsDto getDashboardStats(Long sessionId) {
        Long schoolId = SecurityUtil.schoolId();
        AcademicSession sessionModel = sessionRepository.findById(sessionId).orElse(null);
        String sessionName = sessionModel != null ? sessionModel.getName() : "";

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
            totalPending += getStudentFeeSummary(student.getId(), sessionId).getPendingFee();
        }

        return FeeStatsDto.builder()
                .todayCollection(todayPaid)
                .totalStudents(totalStudents)
                .pendingDues(totalPending)
                .build();
    }

    @Transactional(readOnly = true)
    public FeeSummaryDto getStudentFeeSummary(Long studentId, Long sessionId) {

        if (!studentRepository.existsById(studentId)) {
            throw new ResourceNotFoundException("Student not found: " + studentId);
        }

        AcademicSession sessionModel = sessionRepository.findById(sessionId).orElse(null);
        String sessionName = sessionModel != null ? sessionModel.getName() : "";

        // 1. Fetch assigned fee structures
        List<StudentFeeAssignment> assignments = assignmentRepository.findByStudentIdAndSessionId(studentId,
                sessionId);

        // 2. Calculate Total Accrued Fee (Using assignment.amount directly)
        long totalFeeAccrued = 0;

        for (StudentFeeAssignment assignment : assignments) {
            // In the new logic, assignment.amount already contains the total due for the
            // frequency
            // (e.g., monthly * 12 for MONTHLY, or structure amount for ONE_TIME/ANNUALLY)
            totalFeeAccrued += (long) assignment.getAmount();
        }

        // 3. Calculate total paid
        long totalPaid = paymentRepository.findByStudentId(studentId)
                .stream()
                .filter(p -> sessionId.equals(p.getSessionId()))
                .mapToLong(FeePayment::getAmountPaid)
                .sum();

        // 4. Pending
        long pending = Math.max(totalFeeAccrued - totalPaid, 0);

        // 5. Build response
        FeeSummaryDto dto = new FeeSummaryDto();
        dto.setStudentId(studentId);

        studentRepository.findById(studentId).ifPresent(
                s -> dto.setStudentName(s.getFirstName() + " " +
                        (s.getLastName() != null ? s.getLastName() : "")));

        dto.setSession(sessionName); // DTO still has String session
        dto.setTotalFee((int) totalFeeAccrued);
        dto.setTotalPaid((int) totalPaid);
        dto.setPendingFee((int) pending);
        dto.setFeePending(pending > 0);

        return dto;
    }

    @Transactional(readOnly = true)
    public List<FeeSummaryDto> getTopDefaulters(Long sessionId, int limit) {
        Long schoolId = SecurityUtil.schoolId();
        List<Student> students = studentRepository
                .findBySchoolId(schoolId, Pageable.unpaged())
                .getContent();

        return students.stream()
                .map(s -> getStudentFeeSummary(s.getId(), sessionId))
                .filter(dto -> dto.getPendingFee() > 0)
                .sorted((a, b) -> Integer.compare(b.getPendingFee(), a.getPendingFee()))
                .limit(limit)
                .toList();
    }

    /**
     * Returns all students with outstanding fee balance for the current session.
     * Enriches data with class info, days overdue, and parent contact.
     */
    @Transactional(readOnly = true)
    public List<DefaulterDto> getAllDefaulters() {
        Long schoolId = SecurityUtil.schoolId();

        // Auto-detect the current session from School
        com.school.backend.school.entity.School school = schoolRepository.findById(schoolId).orElse(null);
        if (school == null || school.getCurrentSessionId() == null) {
            return List.of();
        }

        Long currentSessionId = school.getCurrentSessionId();
        AcademicSession sessionModel = sessionRepository.findById(currentSessionId).orElse(null);
        String sessionName = sessionModel != null ? sessionModel.getName() : "";

        // Parse session start for overdue calculation
        // Only if sessionName is valid format "YYYY-YY"
        LocalDate sessionStart;
        try {
            int startYear = Integer.parseInt(sessionName.split("-")[0]);
            sessionStart = LocalDate.of(startYear, Month.APRIL, 1);
        } catch (Exception e) {
            sessionStart = LocalDate.now().minusMonths(6); // Fallback
        }

        LocalDate today = LocalDate.now();

        List<Student> students = studentRepository
                .findBySchoolId(schoolId, Pageable.unpaged())
                .getContent();

        List<DefaulterDto> defaulters = new ArrayList<>();

        for (Student student : students) {
            FeeSummaryDto summary = getStudentFeeSummary(student.getId(), currentSessionId);
            if (summary.getPendingFee() <= 0) {
                continue;
            }

            // Last payment date
            LocalDate lastPaymentDate = paymentRepository
                    .findTopByStudentIdOrderByPaymentDateDesc(student.getId())
                    .map(FeePayment::getPaymentDate)
                    .orElse(null);

            // Days overdue: from last payment or session start
            LocalDate overdueFrom = lastPaymentDate != null ? lastPaymentDate : sessionStart;
            long daysOverdue = Math.max(ChronoUnit.DAYS.between(overdueFrom, today), 0);

            // Class info
            String className = "";
            String classSection = "";
            if (student.getCurrentClass() != null) {
                className = student.getCurrentClass().getName();
                classSection = student.getCurrentClass().getSection();
            }

            defaulters.add(DefaulterDto.builder()
                    .studentId(student.getId())
                    .studentName(student.getFirstName() + " "
                            + (student.getLastName() != null ? student.getLastName() : ""))
                    .admissionNumber(student.getAdmissionNumber())
                    .className(className)
                    .classSection(classSection != null ? classSection : "")
                    .amountDue(summary.getPendingFee())
                    .lastPaymentDate(lastPaymentDate)
                    .daysOverdue(daysOverdue)
                    .parentContact(student.getContactNumber() != null ? student.getContactNumber()
                            : "")
                    .build());
        }

        // Sort by amount due descending
        defaulters.sort(Comparator.comparingLong(DefaulterDto::getAmountDue).reversed());
        return defaulters;
    }
}
