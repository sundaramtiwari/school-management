package com.school.backend.fee;

import com.school.backend.common.BaseAuthenticatedIntegrationTest;
import com.school.backend.common.enums.FeeFrequency;
import com.school.backend.common.enums.Gender;
import com.school.backend.core.guardian.dto.GuardianCreateRequest;
import com.school.backend.core.student.dto.StudentCreateRequest;
import com.school.backend.core.student.dto.StudentDto;
import com.school.backend.fee.dto.FeePaymentAllocationRequest;
import com.school.backend.fee.dto.FeePaymentRequest;
import com.school.backend.fee.dto.FeePaymentDto;
import com.school.backend.fee.dto.StudentLedgerDto;
import com.school.backend.fee.dto.FeeSummaryDto;
import com.school.backend.fee.dto.FeeStructureDto;
import com.school.backend.fee.dto.StudentFeeAssignmentDto;
import com.school.backend.fee.dto.StudentFeeAssignRequest;
import com.school.backend.fee.dto.FeeStructureCreateRequest;
import com.school.backend.fee.entity.FeeType;
import com.school.backend.fee.repository.StudentFeeAssignmentRepository;
import com.school.backend.school.entity.AcademicSession;
import com.school.backend.school.entity.School;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.Objects;

public class StudentLedgerAggregationTest extends BaseAuthenticatedIntegrationTest {

        private Long schoolId;
        private Long studentId;
        private Long feeTypeId;

        @Autowired
        private JdbcTemplate jdbcTemplate;

        @Autowired
        private ObjectMapper objectMapper;

        @Autowired
        private StudentFeeAssignmentRepository assignmentRepository;

        @Test
        void ledger_aggregation_should_be_accurate_and_deterministic() throws Exception {
                setupSchoolAndLogin();

                // Fee Type
                FeeType typeReq = new FeeType();
                typeReq.setName("T");
                ResponseEntity<FeeType> ftResp = restTemplate
                                .exchange("/api/fees/types", HttpMethod.POST, new HttpEntity<>(typeReq, headers),
                                                FeeType.class);
                Assertions.assertThat(ftResp.getStatusCode()).isEqualTo(HttpStatus.OK);
                Assertions.assertThat(ftResp.getBody()).isNotNull();
                feeTypeId = ftResp.getBody().getId();

                // Session A: 50000 assigned, 40000 paid
                Long s1Id = createSession("2023-24", 2023);
                Long fs1Id = createFeeStructure(s1Id, BigDecimal.valueOf(50000));

                // Student
                StudentCreateRequest sreq = new StudentCreateRequest();
                sreq.setAdmissionNumber("L-1");
                sreq.setFirstName("L");
                sreq.setGender(Gender.MALE);
                sreq.setGuardians(List.of(GuardianCreateRequest.builder().name("G").contactNumber("1234567890")
                                .relation("FATHER").primaryGuardian(true).build()));
                ResponseEntity<String> sResp = restTemplate
                                .exchange("/api/students", HttpMethod.POST, new HttpEntity<>(sreq, headers),
                                                String.class);
                Assertions.assertThat(sResp.getBody()).isNotNull();
                if (sResp.getStatusCode() != HttpStatus.OK && sResp.getStatusCode() != HttpStatus.CREATED) {
                        throw new RuntimeException("Student creation failed with " + sResp.getStatusCode() + ": "
                                        + sResp.getBody());
                }
                studentId = objectMapper.readValue(sResp.getBody(), StudentDto.class).getId();

                assignFee(studentId, fs1Id, s1Id);
                payFee(studentId, s1Id, BigDecimal.valueOf(40000));

                // Session B: 55000 assigned, 0 paid
                Long s2Id = createSession("2024-25", 2024);
                Long fs2Id = createFeeStructure(s2Id, BigDecimal.valueOf(55000));
                assignFee(studentId, fs2Id, s2Id);

                // Session C: 60000 assigned, 5000 discount, 10000 funding, 20000 paid
                Long s3Id = createSession("2025-26", 2025);
                Long fs3Id = createFeeStructure(s3Id, BigDecimal.valueOf(60000));
                Long assignCId = assignFee(studentId, fs3Id, s3Id);

                // Apply discount and funding manually on the assignment to match required state
                // Use jdbcTemplate to bypass JPA filters/session issues.
                jdbcTemplate.update(
                                "UPDATE student_fee_assignments SET total_discount_amount = 5000, sponsor_covered_amount = 10000 WHERE id = ?",
                                assignCId);

                payFee(studentId, s3Id, BigDecimal.valueOf(20000));

                // Validate Grand Ledger
                String url = "/api/fees/summary/students/" + studentId + "/ledger";
                ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET,
                                new HttpEntity<>(headers),
                                String.class);
                Assertions.assertThat(resp.getBody()).isNotNull();
                if (resp.getStatusCode() != HttpStatus.OK) {
                        throw new RuntimeException(
                                        "API Failed with status " + resp.getStatusCode() + ". Raw Body: "
                                                        + resp.getBody());
                }
                Assertions.assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);

                StudentLedgerDto ledger = objectMapper.readValue(resp.getBody(), StudentLedgerDto.class);
                Assertions.assertThat(ledger).isNotNull();

                // Totals:
                // A: 50000 fee, 40000 paid -> 10000 pending
                // B: 55000 fee, 0 paid -> 55000 pending
                // C: 60000 fee (45000 net after 5000 disc, 10000 funding), 20000 paid -> 25000
                // pending
                // Grand Total Fee: 50000 + 55000 + 60000 = 165000 (wait, usually Total Fee
                // refers to gross or net? Let's check DTO)
                // In getStudentFullLedger: summary.setTotalFee(totalFee) where totalFee is
                // stats[1] (gross assigned amount).

                Assertions.assertThat(ledger.getGrandTotalFee()).isEqualByComparingTo(BigDecimal.valueOf(165000));
                Assertions.assertThat(ledger.getGrandTotalPaid()).isEqualByComparingTo(BigDecimal.valueOf(60000));
                Assertions.assertThat(ledger.getGrandTotalPending()).isEqualByComparingTo(BigDecimal.valueOf(90000)); // 10k
                // +
                // 55k
                // +
                // 25k

                // Validate individual session C details if possible (though DTO might be
                // limited)
                FeeSummaryDto cSummary = ledger.getSessionSummaries().stream()
                                .filter(s -> s.getSession().equals("2025-26"))
                                .findFirst()
                                .orElseThrow(() -> new AssertionError("Session 2025-26 summary not found"));
                Assertions.assertThat(cSummary.getTotalFee()).isEqualByComparingTo(BigDecimal.valueOf(60000));
                Assertions.assertThat(cSummary.getTotalPaid()).isEqualByComparingTo(BigDecimal.valueOf(20000));
                Assertions.assertThat(cSummary.getPendingFee()).isEqualByComparingTo(BigDecimal.valueOf(25000));
        }

        private void setupSchoolAndLogin() {
                Map<String, Object> schoolReq = Map.of("name", "Ledger School", "displayName", "LS", "board", "CBSE",
                                "schoolCode", "LS-1", "city", "V", "state", "UP");
                ResponseEntity<School> schoolResp = restTemplate.exchange("/api/schools", HttpMethod.POST,
                                new HttpEntity<>(schoolReq, headers), School.class);
                schoolId = Objects.requireNonNull(schoolResp.getBody()).getId();
                loginAsSchoolAdmin(schoolId);
        }

        private Long createSession(String name, int startYear) {
                AcademicSession session = sessionRepository.save(AcademicSession.builder()
                                .name(name).startDate(LocalDate.of(startYear, 4, 1))
                                .endDate(LocalDate.of(startYear + 1, 3, 31))
                                .schoolId(schoolId).active(true).build());
                School school = schoolRepository.findById(schoolId).get();
                school.setCurrentSessionId(session.getId());
                schoolRepository.save(school);
                return session.getId();
        }

        private Long createFeeStructure(Long sessionId, BigDecimal amount) {
                // We need a class
                Map<String, Object> req = Map.of("name", "C", "sessionId", sessionId, "schoolId", schoolId);
                setSessionHeader(sessionId);
                Long classId = Long.valueOf(
                                Objects.requireNonNull(restTemplate.exchange("/api/classes", HttpMethod.POST,
                                                new HttpEntity<>(req, headers),
                                                Map.class)
                                                .getBody()).get("id").toString());

                FeeStructureCreateRequest fsReq = new FeeStructureCreateRequest();
                fsReq.setClassId(classId);
                fsReq.setSessionId(sessionId);
                fsReq.setFeeTypeId(feeTypeId);
                fsReq.setAmount(amount);
                fsReq.setFrequency(FeeFrequency.ONE_TIME);
                ResponseEntity<FeeStructureDto> resp = restTemplate.exchange("/api/fees/structures", HttpMethod.POST,
                                new HttpEntity<>(fsReq, headers),
                                FeeStructureDto.class);
                Assertions.assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
                Assertions.assertThat(resp.getBody()).isNotNull();
                return resp.getBody().getId();
        }

        private Long assignFee(Long studentId, Long fsId, Long sessionId) {
                StudentFeeAssignRequest req = new StudentFeeAssignRequest();
                req.setStudentId(studentId);
                req.setFeeStructureId(fsId);
                req.setSessionId(sessionId);
                setSessionHeader(sessionId);
                ResponseEntity<StudentFeeAssignmentDto> resp = restTemplate.exchange("/api/fees/assignments",
                                HttpMethod.POST,
                                new HttpEntity<>(req, headers), StudentFeeAssignmentDto.class);
                Assertions.assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
                Assertions.assertThat(resp.getBody()).isNotNull();
                return resp.getBody().getId();
        }

        private void payFee(Long studentId, Long sessionId, BigDecimal amount) {
                Long assignmentId = assignmentRepository.findByStudentIdAndSessionId(studentId, sessionId)
                                .get(0).getId();

                FeePaymentRequest req = new FeePaymentRequest();
                req.setStudentId(studentId);
                req.setSessionId(sessionId);
                req.setAllocations(List.of(
                                FeePaymentAllocationRequest.builder()
                                                .assignmentId(assignmentId)
                                                .principalAmount(amount)
                                                .build()));
                req.setMode("CASH");
                setSessionHeader(sessionId);
                ResponseEntity<Map<String, Object>> resp = restTemplate.exchange("/api/fees/payments", HttpMethod.POST,
                                new HttpEntity<>(req, headers),
                                new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {
                                });
                Assertions.assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @AfterEach
        void cleanup() {
                fullCleanup();
        }
}
