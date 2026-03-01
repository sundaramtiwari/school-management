package com.school.backend.core.student;

import com.school.backend.common.BaseAuthenticatedIntegrationTest;
import com.school.backend.common.enums.FeeFrequency;
import com.school.backend.common.enums.Gender;
import com.school.backend.common.enums.StudentStatus;
import com.school.backend.core.classsubject.entity.SchoolClass;
import com.school.backend.core.student.dto.StudentWithdrawalRequest;
import com.school.backend.core.student.dto.StudentWithdrawalResponse;
import com.school.backend.core.student.entity.Student;
import com.school.backend.core.student.entity.StudentEnrollment;
import com.school.backend.fee.entity.FeeStructure;
import com.school.backend.fee.entity.FeeType;
import com.school.backend.fee.entity.StudentFeeAssignment;
import com.school.backend.school.entity.School;
import com.school.backend.transport.entity.PickupPoint;
import com.school.backend.transport.entity.TransportEnrollment;
import com.school.backend.transport.entity.TransportRoute;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;

public class StudentWithdrawalIntegrationTest extends BaseAuthenticatedIntegrationTest {

    @Test
    void withdraw_midSession_noPayments_shouldDeactivateOnlyFutureUnpaidAssignments() {
        Long schoolId = createSchoolAndLogin("W-1");
        Long sessionId = setupSession(schoolId, sessionRepository, schoolRepository);
        setSessionHeader(sessionId);

        SchoolClass schoolClass = createClass(schoolId, sessionId, "10", "A");
        Student student = createStudent(schoolId, "W-STD-1");
        StudentEnrollment enrollment = createEnrollment(schoolId, sessionId, student.getId(), schoolClass.getId(), true);
        FeeStructure structure = createFeeStructure(schoolId, sessionId, schoolClass.getId(), "TUITION-1");

        LocalDate withdrawalDate = LocalDate.of(2025, 9, 15);
        StudentFeeAssignment future = createAssignment(
                schoolId, sessionId, student.getId(), structure.getId(), LocalDate.of(2025, 10, 10),
                BigDecimal.ZERO, BigDecimal.ZERO, true);
        StudentFeeAssignment past = createAssignment(
                schoolId, sessionId, student.getId(), structure.getId(), LocalDate.of(2025, 8, 10),
                BigDecimal.ZERO, BigDecimal.ZERO, true);
        StudentFeeAssignment nullDue = createAssignment(
                schoolId, sessionId, student.getId(), structure.getId(), null,
                BigDecimal.ZERO, BigDecimal.ZERO, true);

        ResponseEntity<StudentWithdrawalResponse> response = withdraw(student.getId(), sessionId, withdrawalDate, "Left school");

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        StudentWithdrawalResponse body = Objects.requireNonNull(response.getBody());
        Assertions.assertThat(body.isEnrollmentClosed()).isTrue();
        Assertions.assertThat(body.getFutureAssignmentsDeactivated()).isEqualTo(1);
        Assertions.assertThat(body.getFutureAssignmentsSkippedDueToPayment()).isEqualTo(0);
        Assertions.assertThat(body.getSkippedAssignmentIds()).isEmpty();
        Assertions.assertThat(body.isTransportUnenrolled()).isFalse();

        StudentEnrollment after = studentEnrollmentRepository.findById(enrollment.getId()).orElseThrow();
        Assertions.assertThat(after.isActive()).isFalse();
        Assertions.assertThat(after.getEndDate()).isEqualTo(withdrawalDate);
        Student studentAfter = studentRepository.findById(student.getId()).orElseThrow();
        Assertions.assertThat(studentAfter.getCurrentStatus()).isEqualTo(StudentStatus.LEFT);

        Assertions.assertThat(assignmentRepository.findById(future.getId()).orElseThrow().isActive()).isFalse();
        Assertions.assertThat(assignmentRepository.findById(past.getId()).orElseThrow().isActive()).isTrue();
        Assertions.assertThat(assignmentRepository.findById(nullDue.getId()).orElseThrow().isActive()).isTrue();
    }

    @Test
    void withdraw_midSession_partialFuturePayment_shouldSkipDeactivationAndReturnSkippedIds() {
        Long schoolId = createSchoolAndLogin("W-2");
        Long sessionId = setupSession(schoolId, sessionRepository, schoolRepository);

        SchoolClass schoolClass = createClass(schoolId, sessionId, "9", "B");
        Student student = createStudent(schoolId, "W-STD-2");
        createEnrollment(schoolId, sessionId, student.getId(), schoolClass.getId(), true);
        FeeStructure structure = createFeeStructure(schoolId, sessionId, schoolClass.getId(), "TUITION-2");

        StudentFeeAssignment futurePartiallyPaid = createAssignment(
                schoolId, sessionId, student.getId(), structure.getId(), LocalDate.of(2025, 11, 10),
                BigDecimal.valueOf(100), BigDecimal.ZERO, true);

        ResponseEntity<StudentWithdrawalResponse> response = withdraw(
                student.getId(), sessionId, LocalDate.of(2025, 9, 20), "Transfer");

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        StudentWithdrawalResponse body = Objects.requireNonNull(response.getBody());
        Assertions.assertThat(body.isEnrollmentClosed()).isTrue();
        Assertions.assertThat(body.getFutureAssignmentsDeactivated()).isEqualTo(0);
        Assertions.assertThat(body.getFutureAssignmentsSkippedDueToPayment()).isEqualTo(1);
        Assertions.assertThat(body.getSkippedAssignmentIds()).containsExactly(futurePartiallyPaid.getId());

        Assertions.assertThat(assignmentRepository.findById(futurePartiallyPaid.getId()).orElseThrow().isActive())
                .isTrue();
    }

    @Test
    void withdraw_whenTransportActive_shouldUnenrollTransport() {
        Long schoolId = createSchoolAndLogin("W-3");
        Long sessionId = setupSession(schoolId, sessionRepository, schoolRepository);

        SchoolClass schoolClass = createClass(schoolId, sessionId, "8", "A");
        Student student = createStudent(schoolId, "W-STD-3");
        createEnrollment(schoolId, sessionId, student.getId(), schoolClass.getId(), true);
        createFeeStructure(schoolId, sessionId, schoolClass.getId(), "TUITION-3");

        TransportRoute route = transportRouteRepository.save(TransportRoute.builder()
                .schoolId(schoolId)
                .name("Route 1")
                .capacity(30)
                .currentStrength(1)
                .active(true)
                .build());
        PickupPoint pickup = pickupPointRepository.save(PickupPoint.builder()
                .schoolId(schoolId)
                .name("Stop 1")
                .amount(BigDecimal.valueOf(500))
                .frequency(FeeFrequency.MONTHLY)
                .route(route)
                .build());
        transportEnrollmentRepository.save(TransportEnrollment.builder()
                .schoolId(schoolId)
                .studentId(student.getId())
                .sessionId(sessionId)
                .pickupPoint(pickup)
                .active(true)
                .build());

        ResponseEntity<StudentWithdrawalResponse> response = withdraw(
                student.getId(), sessionId, LocalDate.of(2025, 9, 30), "Withdrew");

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        StudentWithdrawalResponse body = Objects.requireNonNull(response.getBody());
        Assertions.assertThat(body.isTransportUnenrolled()).isTrue();

        TransportEnrollment enrollment = transportEnrollmentRepository
                .findByStudentIdAndSessionIdAndSchoolId(student.getId(), sessionId, schoolId)
                .orElseThrow();
        Assertions.assertThat(enrollment.isActive()).isFalse();
        Assertions.assertThat(transportRouteRepository.findById(route.getId()).orElseThrow().getCurrentStrength())
                .isEqualTo(0);
    }

    @Test
    void withdraw_whenEnrollmentAlreadyInactive_shouldReturnNoOp() {
        Long schoolId = createSchoolAndLogin("W-4");
        Long sessionId = setupSession(schoolId, sessionRepository, schoolRepository);

        SchoolClass schoolClass = createClass(schoolId, sessionId, "7", "A");
        Student student = createStudent(schoolId, "W-STD-4");
        createEnrollment(schoolId, sessionId, student.getId(), schoolClass.getId(), false);
        FeeStructure structure = createFeeStructure(schoolId, sessionId, schoolClass.getId(), "TUITION-4");
        StudentFeeAssignment future = createAssignment(
                schoolId, sessionId, student.getId(), structure.getId(), LocalDate.of(2025, 10, 10),
                BigDecimal.ZERO, BigDecimal.ZERO, true);

        ResponseEntity<StudentWithdrawalResponse> response = withdraw(
                student.getId(), sessionId, LocalDate.of(2025, 9, 1), "Already inactive");

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        StudentWithdrawalResponse body = Objects.requireNonNull(response.getBody());
        Assertions.assertThat(body.isEnrollmentClosed()).isFalse();
        Assertions.assertThat(body.getFutureAssignmentsDeactivated()).isEqualTo(0);
        Assertions.assertThat(body.getFutureAssignmentsSkippedDueToPayment()).isEqualTo(0);
        Assertions.assertThat(body.getSkippedAssignmentIds()).isEmpty();
        Assertions.assertThat(body.isTransportUnenrolled()).isFalse();
        Assertions.assertThat(assignmentRepository.findById(future.getId()).orElseThrow().isActive()).isTrue();
    }

    @Test
    void withdraw_whenEnrollmentNotFound_shouldReturn404() {
        Long schoolId = createSchoolAndLogin("W-5");
        Long sessionId = setupSession(schoolId, sessionRepository, schoolRepository);
        Student student = createStudent(schoolId, "W-STD-5");

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/students/" + student.getId() + "/withdraw",
                HttpMethod.POST,
                new HttpEntity<>(buildWithdrawRequest(sessionId, LocalDate.of(2025, 9, 10), "No enrollment"), headers),
                Map.class);

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void withdraw_whenWithdrawalDateOutsideSession_shouldReturn400() {
        Long schoolId = createSchoolAndLogin("W-6");
        Long sessionId = setupSession(schoolId, sessionRepository, schoolRepository);

        SchoolClass schoolClass = createClass(schoolId, sessionId, "6", "C");
        Student student = createStudent(schoolId, "W-STD-6");
        createEnrollment(schoolId, sessionId, student.getId(), schoolClass.getId(), true);

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/students/" + student.getId() + "/withdraw",
                HttpMethod.POST,
                new HttpEntity<>(buildWithdrawRequest(sessionId, LocalDate.of(2024, 12, 31), "Invalid date"), headers),
                Map.class);

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void withdraw_shouldRespectTenantIsolation() {
        Long schoolAId = createSchoolAndLogin("W-7-A");
        Long sessionAId = setupSession(schoolAId, sessionRepository, schoolRepository);
        SchoolClass classA = createClass(schoolAId, sessionAId, "5", "A");
        Student studentA = createStudent(schoolAId, "W-STD-7");
        createEnrollment(schoolAId, sessionAId, studentA.getId(), classA.getId(), true);

        Long schoolBId = createSchoolAndLogin("W-7-B");
        setupSession(schoolBId, sessionRepository, schoolRepository);

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/students/" + studentA.getId() + "/withdraw",
                HttpMethod.POST,
                new HttpEntity<>(buildWithdrawRequest(sessionAId, LocalDate.of(2025, 9, 10), "Cross-tenant"), headers),
                Map.class);

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private Long createSchoolAndLogin(String codeSuffix) {
        loginAsSuperAdmin();
        Map<String, Object> schoolReq = Map.of(
                "name", "Withdrawal School " + codeSuffix,
                "displayName", "WS-" + codeSuffix,
                "board", "CBSE",
                "schoolCode", "WS-" + codeSuffix,
                "city", "V",
                "state", "UP");
        ResponseEntity<School> schoolResp = restTemplate.exchange(
                "/api/schools", HttpMethod.POST, new HttpEntity<>(schoolReq, headers), School.class);
        Long schoolId = Objects.requireNonNull(schoolResp.getBody()).getId();
        loginAsSchoolAdmin(schoolId);
        return schoolId;
    }

    private SchoolClass createClass(Long schoolId, Long sessionId, String name, String section) {
        return schoolClassRepository.save(SchoolClass.builder()
                .schoolId(schoolId)
                .sessionId(sessionId)
                .name(name)
                .section(section)
                .active(true)
                .build());
    }

    private Student createStudent(Long schoolId, String admissionNumber) {
        return studentRepository.save(Student.builder()
                .schoolId(schoolId)
                .admissionNumber(admissionNumber)
                .firstName("Stu")
                .gender(Gender.MALE)
                .active(true)
                .currentStatus(StudentStatus.ENROLLED)
                .build());
    }

    private StudentEnrollment createEnrollment(Long schoolId, Long sessionId, Long studentId, Long classId, boolean active) {
        return studentEnrollmentRepository.save(StudentEnrollment.builder()
                .schoolId(schoolId)
                .sessionId(sessionId)
                .studentId(studentId)
                .classId(classId)
                .section("A")
                .enrollmentDate(LocalDate.of(2025, 4, 1))
                .startDate(LocalDate.of(2025, 4, 1))
                .active(active)
                .build());
    }

    private FeeStructure createFeeStructure(Long schoolId, Long sessionId, Long classId, String typeName) {
        FeeType feeType = feeTypeRepository.save(FeeType.builder()
                .schoolId(schoolId)
                .name(typeName)
                .active(true)
                .build());
        return feeStructureRepository.save(FeeStructure.builder()
                .schoolId(schoolId)
                .classId(classId)
                .sessionId(sessionId)
                .feeType(feeType)
                .amount(BigDecimal.valueOf(1000))
                .frequency(FeeFrequency.MONTHLY)
                .active(true)
                .build());
    }

    private StudentFeeAssignment createAssignment(
            Long schoolId,
            Long sessionId,
            Long studentId,
            Long feeStructureId,
            LocalDate dueDate,
            BigDecimal principalPaid,
            BigDecimal lateFeePaid,
            boolean active) {
        return assignmentRepository.save(StudentFeeAssignment.builder()
                .schoolId(schoolId)
                .sessionId(sessionId)
                .studentId(studentId)
                // Ensure uniqueness on (student, fee_structure, session) for tests
                .feeStructureId(resolveUniqueFeeStructureId(schoolId, sessionId, studentId, feeStructureId))
                .amount(BigDecimal.valueOf(1000))
                .dueDate(dueDate)
                .principalPaid(principalPaid)
                .lateFeePaid(lateFeePaid)
                .active(active)
                .build());
    }

    private Long resolveUniqueFeeStructureId(Long schoolId, Long sessionId, Long studentId, Long feeStructureId) {
        boolean exists = assignmentRepository.existsByStudentIdAndFeeStructureIdAndSessionId(
                studentId, feeStructureId, sessionId);
        if (!exists) {
            return feeStructureId;
        }
        FeeStructure original = feeStructureRepository.findById(feeStructureId).orElseThrow();
        FeeStructure cloned = feeStructureRepository.save(FeeStructure.builder()
                .schoolId(schoolId)
                .classId(original.getClassId())
                .sessionId(sessionId)
                .feeType(original.getFeeType())
                .amount(original.getAmount())
                .frequency(original.getFrequency())
                .active(true)
                .build());
        return cloned.getId();
    }

    private ResponseEntity<StudentWithdrawalResponse> withdraw(
            Long studentId,
            Long sessionId,
            LocalDate withdrawalDate,
            String reason) {
        return restTemplate.exchange(
                "/api/students/" + studentId + "/withdraw",
                HttpMethod.POST,
                new HttpEntity<>(buildWithdrawRequest(sessionId, withdrawalDate, reason), headers),
                StudentWithdrawalResponse.class);
    }

    private StudentWithdrawalRequest buildWithdrawRequest(Long sessionId, LocalDate withdrawalDate, String reason) {
        StudentWithdrawalRequest request = new StudentWithdrawalRequest();
        request.setSessionId(sessionId);
        request.setWithdrawalDate(withdrawalDate);
        request.setStatus(StudentStatus.LEFT);
        request.setReason(reason);
        return request;
    }

    @AfterEach
    void cleanup() {
        fullCleanup();
    }
}
