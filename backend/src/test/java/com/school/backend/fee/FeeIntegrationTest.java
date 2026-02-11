package com.school.backend.fee;

import com.school.backend.common.BaseAuthenticatedIntegrationTest;
import com.school.backend.common.enums.Gender;
import com.school.backend.core.classsubject.entity.SchoolClass;
import com.school.backend.core.classsubject.repository.SchoolClassRepository;
import com.school.backend.core.student.entity.Student;
import com.school.backend.core.student.repository.StudentRepository;
import com.school.backend.fee.dto.*;
import com.school.backend.fee.entity.FeePayment;
import com.school.backend.fee.entity.FeeType;
import com.school.backend.fee.enums.FeeFrequency;
import com.school.backend.fee.repository.FeePaymentRepository;
import com.school.backend.fee.repository.FeeStructureRepository;
import com.school.backend.fee.repository.FeeTypeRepository;
import com.school.backend.fee.repository.StudentFeeAssignmentRepository;
import com.school.backend.school.entity.School;
import com.school.backend.school.repository.SchoolRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

public class FeeIntegrationTest extends BaseAuthenticatedIntegrationTest {

        @Autowired
        private SchoolRepository schoolRepository;
        @Autowired
        private FeeStructureRepository feeStructureRepository;
        @Autowired
        private FeeTypeRepository feeTypeRepository;
        @Autowired
        private FeePaymentRepository feePaymentRepository;
        @Autowired
        private StudentFeeAssignmentRepository assignmentRepository;
        @Autowired
        private SchoolClassRepository classRepository;
        @Autowired
        private StudentRepository studentRepository;

        private School testSchool;
        private Long feeTypeId;
        private Long classId;
        private Long studentId;

        @BeforeEach
        void setupFeeTest() {
                cleanup();

                // Ensure we are logged in as a school admin
                testSchool = schoolRepository.findBySchoolCode("SPS001")
                                .orElseGet(() -> {
                                        School s = new School();
                                        s.setName("Test School");
                                        s.setSchoolCode("SPS001");
                                        s.setActive(true);
                                        return schoolRepository.save(s);
                                });

                loginAsSchoolAdmin(testSchool.getId());

                // Create a Class
                var cls = new SchoolClass();
                cls.setName("X");
                cls.setSection("A");
                cls.setSession("2024-25");
                cls.setSchoolId(testSchool.getId());
                cls.setActive(true);
                cls = classRepository.save(cls);
                classId = cls.getId();

                // Create a Student
                var student = new Student();
                student.setFirstName("Test");
                student.setLastName("Student");
                student.setAdmissionNumber("ADM001");
                student.setGender(Gender.MALE);
                student.setSchoolId(testSchool.getId());
                student.setCurrentClass(cls);
                student.setActive(true);
                student = studentRepository.save(student);
                studentId = student.getId();

                // Create a Fee Type
                var ft = new FeeType();
                ft.setName("Tuition Fee");
                ft.setSchoolId(testSchool.getId());
                ft.setActive(true);
                feeTypeRepository.save(ft);
                feeTypeId = ft.getId();
        }

        @Test
        void fullFeeFlow_CreateStructure_Pay_VerifyBalance() {
                // 1. Create Fee Structure (Monthly)
                FeeStructureCreateRequest createReq = new FeeStructureCreateRequest();
                createReq.setClassId(classId); // Real Class ID
                createReq.setSession("2024-25");
                createReq.setFeeTypeId(feeTypeId);
                createReq.setAmount(5000);
                createReq.setFrequency(FeeFrequency.MONTHLY);

                ResponseEntity<FeeStructureDto> structureResp = restTemplate.postForEntity(
                                "/api/fees/structures",
                                new HttpEntity<>(createReq, headers),
                                FeeStructureDto.class);
                assertEquals(HttpStatus.OK, structureResp.getStatusCode());
                FeeStructureDto structureBody = Objects.requireNonNull(structureResp.getBody());
                assertNotNull(structureBody);
                assertNotNull(structureBody.getId());
                assertEquals(FeeFrequency.MONTHLY, structureBody.getFrequency());

                // 2. Check Dues for a Student (ID: 501)
                // Dues should be calculated based on logic (assuming implementation uses
                // assignment or auto-calc)
                // If auto-calc logic assumes 1 month due, let's see.
                // Note: Logic depends on start date vs current date.

                // 3. Pay Fees
                FeePaymentRequest payReq = new FeePaymentRequest();
                payReq.setStudentId(studentId); // Use real student ID
                payReq.setAmountPaid(5000);
                payReq.setMode("CASH");
                payReq.setPaymentDate(LocalDate.now());
                payReq.setRemarks("Jan Fee");

                ResponseEntity<FeePaymentDto> payResp = restTemplate.postForEntity(
                                "/api/fees/payments",
                                new HttpEntity<>(payReq, headers),
                                FeePaymentDto.class);
                assertEquals(HttpStatus.OK, payResp.getStatusCode());
                FeePaymentDto payBody = Objects.requireNonNull(payResp.getBody());
                assertNotNull(payBody);
                assertNotNull(payBody.getId());

                // 4. Verify History
                ResponseEntity<FeePaymentDto[]> histResp = restTemplate.exchange(
                                "/api/fees/payments/students/" + studentId,
                                HttpMethod.GET,
                                new HttpEntity<>(headers),
                                FeePaymentDto[].class);
                assertEquals(HttpStatus.OK, histResp.getStatusCode());
                FeePaymentDto[] histBody = Objects.requireNonNull(histResp.getBody());
                assertNotNull(histBody);
                assertTrue(histBody.length >= 1);

                // 5. Download Receipt - COMMENTED OUT: Requires OpenPDF dependency
                // TODO: Add OpenPDF dependency to build.gradle to enable PDF receipt generation
                /*
                 * Long paymentId = payResp.getBody().getId();
                 * ResponseEntity<byte[]> receiptResp = restTemplate.exchange(
                 * "/api/fees/payments/" + paymentId + "/receipt",
                 * HttpMethod.GET,
                 * new HttpEntity<>(headers),
                 * byte[].class);
                 * assertEquals(HttpStatus.OK, receiptResp.getStatusCode());
                 * assertTrue(receiptResp.getBody().length > 0,
                 * "PDF content should not be empty");
                 */
        }

        @Test
        void getRecentPayments_ShouldReturnSortedPaginatedResults() {
                // 1. Create multiple payments with different dates
                // Payment 1: Today
                var p1 = FeePayment.builder()
                                .studentId(studentId)
                                .amountPaid(1000)
                                .paymentDate(LocalDate.now())
                                .mode("CASH")
                                .schoolId(testSchool.getId())
                                .build();
                feePaymentRepository.save(p1);

                // Payment 2: Yesterday
                var p2 = FeePayment.builder()
                                .studentId(studentId)
                                .amountPaid(2000)
                                .paymentDate(LocalDate.now().minusDays(1))
                                .mode("UPI")
                                .schoolId(testSchool.getId())
                                .build();
                feePaymentRepository.save(p2);

                // Payment 3: 2 days ago
                var p3 = FeePayment.builder()
                                .studentId(studentId)
                                .amountPaid(3000)
                                .paymentDate(LocalDate.now().minusDays(2))
                                .mode("BANK")
                                .schoolId(testSchool.getId())
                                .build();
                feePaymentRepository.save(p3);

                // 2. Fetch recent payments with limit=2
                ResponseEntity<FeePaymentDto[]> resp = restTemplate.exchange(
                                "/api/fees/payments/recent?limit=2",
                                HttpMethod.GET,
                                new HttpEntity<>(headers),
                                FeePaymentDto[].class);

                assertEquals(HttpStatus.OK, resp.getStatusCode());
                FeePaymentDto[] recent = Objects.requireNonNull(resp.getBody());
                assertNotNull(recent);
                assertEquals(2, recent.length, "Should return only 2 payments due to limit");

                // 3. Verify order (descending by paymentDate)
                assertEquals(1000, recent[0].getAmountPaid(), "First payment should be the most recent (today)");
                assertEquals(2000, recent[1].getAmountPaid(), "Second payment should be yesterday's");
        }

        @Test
        void getRecentPayments_MultiTenancy_ShouldOnlySeeOwnSchoolPayments() {
                // 1. Create payment for Current School (SPS001)
                var p1 = FeePayment.builder()
                                .studentId(studentId)
                                .amountPaid(555)
                                .paymentDate(LocalDate.now())
                                .mode("CASH")
                                .schoolId(testSchool.getId())
                                .build();
                feePaymentRepository.save(p1);

                // 2. Create another school and a payment for it
                School school2 = new School();
                school2.setName("Other School");
                school2.setSchoolCode("OTH002");
                school2.setActive(true);
                school2 = schoolRepository.save(school2);

                var p2 = FeePayment.builder()
                                .studentId(999L) // Dummy student
                                .amountPaid(777)
                                .paymentDate(LocalDate.now())
                                .mode("UPI")
                                .schoolId(school2.getId())
                                .build();
                feePaymentRepository.save(p2);

                // 3. Fetch recent payments as SPS001 Admin
                // (Headers are already set for testSchool in setupFeeTest)
                ResponseEntity<FeePaymentDto[]> resp1 = restTemplate.exchange(
                                "/api/fees/payments/recent",
                                HttpMethod.GET,
                                new HttpEntity<>(headers),
                                FeePaymentDto[].class);

                FeePaymentDto[] body1 = Objects.requireNonNull(resp1.getBody());
                assertNotNull(body1);
                assertEquals(1, body1.length);
                assertEquals(555, body1[0].getAmountPaid());

                // 4. Login as Other School Admin and fetch
                loginAsSchoolAdmin(school2.getId());
                ResponseEntity<FeePaymentDto[]> resp2 = restTemplate.exchange(
                                "/api/fees/payments/recent",
                                HttpMethod.GET,
                                new HttpEntity<>(headers),
                                FeePaymentDto[].class);

                FeePaymentDto[] body2 = Objects.requireNonNull(resp2.getBody());
                assertNotNull(body2);
                assertEquals(1, body2.length);
                assertEquals(777, body2[0].getAmountPaid());
        }

        private void cleanup() {
                feePaymentRepository.deleteAllInBatch();
                assignmentRepository.deleteAllInBatch();
                feeStructureRepository.deleteAllInBatch();
                feeTypeRepository.deleteAllInBatch();
                studentRepository.deleteAllInBatch();
                classRepository.deleteAllInBatch();
        }

        @org.junit.jupiter.api.AfterEach
        void tearDown() {
                cleanup();
        }
}
