package com.school.backend.transport;

import com.school.backend.common.BaseAuthenticatedIntegrationTest;
import com.school.backend.common.enums.Gender;
import com.school.backend.core.classsubject.entity.SchoolClass;
import com.school.backend.core.classsubject.repository.SchoolClassRepository;
import com.school.backend.core.student.entity.Student;
import com.school.backend.core.student.repository.StudentRepository;
import com.school.backend.fee.dto.FeeSummaryDto;
import com.school.backend.fee.enums.FeeFrequency;
import com.school.backend.fee.repository.FeeStructureRepository;
import com.school.backend.fee.repository.FeeTypeRepository;
import com.school.backend.school.entity.School;
import com.school.backend.school.repository.SchoolRepository;
import com.school.backend.transport.dto.PickupPointDto;
import com.school.backend.transport.dto.TransportEnrollmentDto;
import com.school.backend.transport.dto.TransportRouteDto;
import com.school.backend.transport.entity.PickupPoint;
import com.school.backend.transport.entity.TransportRoute;
import com.school.backend.transport.repository.PickupPointRepository;
import com.school.backend.transport.repository.TransportEnrollmentRepository;
import com.school.backend.transport.repository.TransportRouteRepository;
import com.school.backend.fee.repository.StudentFeeAssignmentRepository;
import com.school.backend.fee.repository.FeePaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Objects;

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

        private School testSchool;
        private SchoolClass testClass;
        private Student testStudent;

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

                testClass = classRepository.save(SchoolClass.builder()
                                .name("X")
                                .section("A")
                                .session("2024-25")
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
                Long routeId = routeResp.getBody().getId();

                // 2. Create Pickup Point (Monthly, 1500)
                PickupPointDto ppReq = PickupPointDto.builder()
                                .name("Stop 1")
                                .amount(1500)
                                .frequency(FeeFrequency.MONTHLY)
                                .routeId(routeId)
                                .build();

                ResponseEntity<PickupPointDto> ppResp = restTemplate.postForEntity(
                                "/api/transport/pickup-points", new HttpEntity<>(ppReq, headers), PickupPointDto.class);
                assertEquals(HttpStatus.OK, ppResp.getStatusCode());
                Long ppId = ppResp.getBody().getId();

                // 3. Enroll Student
                TransportEnrollmentDto enrollReq = TransportEnrollmentDto.builder()
                                .studentId(testStudent.getId())
                                .pickupPointId(ppId)
                                .session("2024-25")
                                .build();

                ResponseEntity<String> enrollResp = restTemplate.postForEntity(
                                "/api/transport/enrollments", new HttpEntity<>(enrollReq, headers), String.class);
                assertEquals(HttpStatus.OK, enrollResp.getStatusCode(), "Enrollment failed: " + enrollResp.getBody());

                // 4. Verify Fee Assignment through Summary
                // Default multiplier for MONTHLY should assume some months passed?
                // Actually FeeSummaryService calculates months passed since start of session.
                // Let's check the FeeSummary API.
                ResponseEntity<FeeSummaryDto> summaryResp = restTemplate.exchange(
                                "/api/fees/summary/students/" + testStudent.getId() + "?session=2024-25",
                                HttpMethod.GET, new HttpEntity<>(headers), FeeSummaryDto.class);

                assertEquals(HttpStatus.OK, summaryResp.getStatusCode());
                FeeSummaryDto summary = summaryResp.getBody();
                assertNotNull(summary);
                assertTrue(summary.getTotalFee() >= 1500, "Transport fee should be accrued");

                // 5. Advance Challan Generation
                // Generate for 3 months
                ResponseEntity<byte[]> challanResp = restTemplate.exchange(
                                "/api/fees/challan/student/" + testStudent.getId() + "?months=3&session=2024-25",
                                HttpMethod.GET, new HttpEntity<>(headers), byte[].class);

                assertEquals(HttpStatus.OK, challanResp.getStatusCode());
                assertNotNull(challanResp.getBody());
                assertTrue(challanResp.getBody().length > 0, "Challan PDF should not be empty");
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
                                .name("Route B").schoolId(testSchool.getId()).build());
                PickupPoint pp = pickupPointRepository.save(PickupPoint.builder()
                                .name("Stop 2").amount(1000).frequency(FeeFrequency.MONTHLY)
                                .route(route).schoolId(testSchool.getId()).build());

                TransportEnrollmentDto enrollReq = TransportEnrollmentDto.builder()
                                .studentId(studentNoClass.getId())
                                .pickupPointId(pp.getId())
                                .session("2024-25")
                                .build();

                ResponseEntity<String> enrollResp = restTemplate.postForEntity(
                                "/api/transport/enrollments", new HttpEntity<>(enrollReq, headers), String.class);

                assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, enrollResp.getStatusCode());
                assertTrue(enrollResp.getBody().contains("Student must be assigned to a class"));
        }

        private void cleanup() {
                paymentRepository.deleteAllInBatch();
                enrollmentRepository.deleteAllInBatch();
                assignmentRepository.deleteAllInBatch();
                pickupPointRepository.deleteAllInBatch();
                routeRepository.deleteAllInBatch();
                studentRepository.deleteAllInBatch();
                classRepository.deleteAllInBatch();
                feeStructureRepository.deleteAllInBatch();
                feeTypeRepository.deleteAllInBatch();
        }

        @org.junit.jupiter.api.AfterEach
        void tearDown() {
                cleanup();
        }
}
