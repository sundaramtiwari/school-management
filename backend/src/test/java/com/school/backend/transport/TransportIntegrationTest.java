package com.school.backend.transport;

import com.school.backend.common.BaseAuthenticatedIntegrationTest;
import com.school.backend.common.enums.Gender;
import com.school.backend.core.classsubject.entity.SchoolClass;
import com.school.backend.core.classsubject.repository.SchoolClassRepository;
import com.school.backend.core.student.entity.Student;
import com.school.backend.core.student.repository.StudentRepository;
import com.school.backend.common.enums.FeeFrequency;
import com.school.backend.fee.repository.FeePaymentRepository;
import com.school.backend.fee.repository.FeeStructureRepository;
import com.school.backend.fee.repository.FeeTypeRepository;
import com.school.backend.fee.repository.StudentFeeAssignmentRepository;
import com.school.backend.school.entity.AcademicSession;
import com.school.backend.school.entity.School;
import com.school.backend.school.repository.AcademicSessionRepository;
import com.school.backend.school.repository.SchoolRepository;
import com.school.backend.transport.dto.PickupPointDto;
import com.school.backend.transport.dto.TransportEnrollmentDto;
import com.school.backend.transport.dto.TransportRouteDto;
import com.school.backend.transport.entity.PickupPoint;
import com.school.backend.transport.entity.TransportRoute;
import com.school.backend.transport.repository.PickupPointRepository;
import com.school.backend.transport.repository.TransportEnrollmentRepository;
import com.school.backend.transport.repository.TransportRouteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;

import static java.lang.Boolean.TRUE;
import static org.junit.jupiter.api.Assertions.*;

public class TransportIntegrationTest extends BaseAuthenticatedIntegrationTest {

    @Autowired
    private SchoolRepository schoolRepository;
    @Autowired
    private SchoolClassRepository classRepository;
    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private TransportRouteRepository routeRepository;
    @Autowired
    private PickupPointRepository pickupPointRepository;
    @Autowired
    private TransportEnrollmentRepository enrollmentRepository;
    @Autowired
    private FeeStructureRepository feeStructureRepository;
    @Autowired
    private FeeTypeRepository feeTypeRepository;
    @Autowired
    private StudentFeeAssignmentRepository assignmentRepository;
    @Autowired
    private FeePaymentRepository paymentRepository;
    @Autowired
    private AcademicSessionRepository sessionRepository;
    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private School testSchool;
    private SchoolClass testClass;
    private Student testStudent;
    private Long sessionId;

    @BeforeEach
    void setup() {
        cleanup();

        testSchool = schoolRepository.findBySchoolCode("SPS001")
                .orElseGet(() -> schoolRepository.save(School.builder()
                        .name("Test School")
                        .schoolCode("SPS001")
                        .active(true)
                        .build()));

        loginAsSchoolAdmin(testSchool.getId());

        // Create Session
        AcademicSession session = sessionRepository.save(AcademicSession.builder()
                .name("2024-25")
                .startDate(LocalDate.of(2024, 4, 1))
                .endDate(LocalDate.of(2025, 3, 31))
                .schoolId(testSchool.getId())
                .active(true)
                .build());
        sessionId = session.getId();

        testClass = classRepository.save(SchoolClass.builder()
                .name("X")
                .section("A")
                .sessionId(sessionId)
                .schoolId(testSchool.getId())
                .active(true)
                .build());

        testStudent = studentRepository.save(Student.builder()
                .firstName("John")
                .lastName("Doe")
                .admissionNumber("ADM001")
                .gender(Gender.MALE)
                .schoolId(testSchool.getId())
                .currentClass(testClass)
                .active(true)
                .build());
    }

    @Test
    void transportFlow_CreateEnrollVerifyFees() {
        // 1. Create Route
        TransportRouteDto routeReq = TransportRouteDto.builder()
                .name("Route A")
                .description("South Campus")
                .build();

        ResponseEntity<TransportRouteDto> routeResp = restTemplate.postForEntity(
                "/api/transport/routes", new HttpEntity<>(routeReq, headers), TransportRouteDto.class);
        assertEquals(HttpStatus.OK, routeResp.getStatusCode());
        assertNotNull(routeResp.getBody());
        Long routeId = routeResp.getBody().getId();

        // 2. Create Pickup Point (Monthly, 1500)
        PickupPointDto ppReq = PickupPointDto.builder()
                .name("Stop 1")
                .amount(java.math.BigDecimal.valueOf(1000)).frequency(FeeFrequency.MONTHLY)
                .routeId(routeId)
                .build();

        ResponseEntity<PickupPointDto> ppResp = restTemplate.postForEntity(
                "/api/transport/pickup-points", new HttpEntity<>(ppReq, headers), PickupPointDto.class);
        assertEquals(HttpStatus.OK, ppResp.getStatusCode());
        assertNotNull(ppResp.getBody());
        Long ppId = ppResp.getBody().getId();

        // 3. Enroll Student
        TransportEnrollmentDto enrollReq = TransportEnrollmentDto.builder()
                .studentId(testStudent.getId())
                .pickupPointId(ppId)
                .sessionId(sessionId)
                .active(true).build();

        ResponseEntity<String> enrollResp = restTemplate.postForEntity(
                "/api/transport/enrollments", new HttpEntity<>(enrollReq, headers), String.class);
        assertEquals(HttpStatus.OK, enrollResp.getStatusCode(), "Enrollment failed: " + enrollResp.getBody());

        // 4. Verify Route Strength Incremented
        TransportRoute routeAfterEnroll = routeRepository.findById(routeId).orElseThrow();
        assertEquals(1, routeAfterEnroll.getCurrentStrength());

        // 5. Unenroll Student
        ResponseEntity<Void> unenrollResp = restTemplate.exchange(
                "/api/transport/enrollments/student/" + testStudent.getId() + "?sessionId=" + sessionId,
                HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);
        assertEquals(HttpStatus.OK, unenrollResp.getStatusCode());

        // 6. Verify Route Strength Decremented
        TransportRoute routeAfterUnenroll = routeRepository.findById(routeId).orElseThrow();
        assertEquals(0, routeAfterUnenroll.getCurrentStrength());

        // 7. Verify Enrollment Inactive
        enrollmentRepository
                .findByStudentIdAndSessionIdAndSchoolId(testStudent.getId(), sessionId,
                        testSchool.getId())
                .ifPresentOrElse(e -> assertFalse(e.isActive()),
                        () -> fail("Enrollment should exist but be inactive"));
    }

    @Test
    void enrollStudent_WithoutClass_ShouldFail() {
        // Create student without class
        Student studentNoClass = studentRepository.save(Student.builder()
                .firstName("No")
                .lastName("Class")
                .admissionNumber("ADM999")
                .gender(Gender.FEMALE)
                .schoolId(testSchool.getId())
                .active(true)
                .build());

        // Create Route & Pickup
        TransportRoute route = routeRepository.save(TransportRoute.builder()
                .name("Route B").schoolId(testSchool.getId()).active(true).build());
        PickupPoint pp = pickupPointRepository.save(PickupPoint.builder()
                .name("Stop 2").amount(java.math.BigDecimal.valueOf(1000))
                .frequency(FeeFrequency.MONTHLY)
                .route(route).schoolId(testSchool.getId()).build());

        TransportEnrollmentDto enrollReq = TransportEnrollmentDto.builder()
                .studentId(studentNoClass.getId())
                .pickupPointId(pp.getId())
                .sessionId(sessionId)
                .active(TRUE).build();

        ResponseEntity<String> enrollResp = restTemplate.postForEntity(
                "/api/transport/enrollments", new HttpEntity<>(enrollReq, headers), String.class);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, enrollResp.getStatusCode());
        assertNotNull(enrollResp.getBody());
        assertTrue(enrollResp.getBody().contains("Student must be assigned to a class"));
    }

    @Test
    void unenroll_DoubleAction_ShouldFailGracefully() {
        // Create Route & Pickup
        TransportRoute route = routeRepository.save(TransportRoute.builder()
                .name("Route C").schoolId(testSchool.getId()).currentStrength(0).capacity(40)
                .active(true).build());
        PickupPoint pp = pickupPointRepository.save(PickupPoint.builder()
                .name("Stop C").amount(java.math.BigDecimal.valueOf(1000))
                .frequency(FeeFrequency.MONTHLY)
                .route(route).schoolId(testSchool.getId()).build());

        // Enroll manually
        restTemplate.postForEntity("/api/transport/enrollments",
                new HttpEntity<>(TransportEnrollmentDto.builder()
                        .studentId(testStudent.getId()).pickupPointId(pp.getId())
                        .sessionId(sessionId).active(true).build(), headers),
                String.class);

        // First unenrollment
        ResponseEntity<Void> resp1 = restTemplate.exchange(
                "/api/transport/enrollments/student/" + testStudent.getId() + "?sessionId=" + sessionId,
                HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);
        assertEquals(HttpStatus.OK, resp1.getStatusCode());

        // Second unenrollment - should return error as it's already inactive
        ResponseEntity<String> resp2 = restTemplate.exchange(
                "/api/transport/enrollments/student/" + testStudent.getId() + "?sessionId=" + sessionId,
                HttpMethod.DELETE, new HttpEntity<>(headers), String.class);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp2.getStatusCode());
        assertNotNull(resp2.getBody());
        assertTrue(resp2.getBody().contains("already unenrolled"));
    }

    private void cleanup() {
        // Delete in order to respect foreign key constraints
        paymentRepository.deleteAllInBatch();
        enrollmentRepository.deleteAllInBatch();
        assignmentRepository.deleteAllInBatch();
        pickupPointRepository.deleteAllInBatch();
        routeRepository.deleteAllInBatch();
        // Delete attendance before students using direct SQL
        try {
            jdbcTemplate.execute("DELETE FROM student_attendance");
        } catch (Exception e) {
            // Ignore if table doesn't exist
        }
        studentRepository.deleteAllInBatch();
        classRepository.deleteAllInBatch();
        feeStructureRepository.deleteAllInBatch();
        feeTypeRepository.deleteAllInBatch();
        sessionRepository.deleteAllInBatch();
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        cleanup();
    }
}
