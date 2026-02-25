package com.school.backend.fee;

import com.school.backend.common.BaseAuthenticatedIntegrationTest;
import com.school.backend.common.enums.FeeFrequency;
import com.school.backend.common.enums.Gender;
import com.school.backend.core.guardian.dto.GuardianCreateRequest;
import com.school.backend.core.student.dto.StudentCreateRequest;
import com.school.backend.core.student.dto.StudentDto;
import com.school.backend.fee.dto.*;
import com.school.backend.fee.entity.FeeType;
import com.school.backend.fee.entity.StudentFeeAssignment;
import com.school.backend.common.enums.LateFeeType;
import com.school.backend.school.entity.School;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class LateFeeAccrualIdempotencyTest extends BaseAuthenticatedIntegrationTest {

        private Long schoolId;
        private Long studentId;
        private Long sessionId;

        @Test
        void late_fee_should_be_idempotent() {
                setupData();

                // 1. Manually update assignment to be OVERDUE and have FLAT late fee
                StudentFeeAssignment assignment = assignmentRepository.findByStudentIdAndSessionId(studentId, sessionId)
                                .get(0);
                assignment.setDueDate(LocalDate.now().minusDays(10));
                assignment.setLateFeeType(LateFeeType.FLAT);
                assignment.setLateFeeValue(BigDecimal.valueOf(500));
                assignment.setLateFeeGraceDays(0);
                assignmentRepository.save(assignment);

                // 2. Call preview (summary) 3 times
                for (int i = 0; i < 3; i++) {
                        ResponseEntity<FeeSummaryDto> summaryResp = restTemplate.exchange(
                                        "/api/fees/summary/students/" + studentId + "?sessionId=" + sessionId,
                                        HttpMethod.GET, new HttpEntity<>(headers), FeeSummaryDto.class);
                        Assertions.assertThat(summaryResp.getStatusCode()).isEqualTo(HttpStatus.OK);
                        // Read path must be persisted-only and should not accrue late fee in real-time
                        Assertions.assertThat(summaryResp.getBody().getTotalLateFeeAccrued())
                                        .isEqualByComparingTo(BigDecimal.ZERO);
                }

                // 3. Ensure no DB mutation yet
                StudentFeeAssignment afterPreview = assignmentRepository.findById(assignment.getId()).get();
                Assertions.assertThat(afterPreview.getLateFeeAccrued()).isEqualByComparingTo(BigDecimal.ZERO);
                Assertions.assertThat(afterPreview.isLateFeeApplied()).isFalse();

                // 4. Perform payment
                FeePaymentRequest payReq = new FeePaymentRequest();
                payReq.setStudentId(studentId);
                payReq.setSessionId(sessionId);
                payReq.setAllocations(List.of(
                                FeePaymentAllocationRequest.builder()
                                                .assignmentId(assignment.getId())
                                                .principalAmount(BigDecimal.valueOf(1000))
                                                .build()));
                payReq.setMode("CASH");

                ResponseEntity<FeePaymentDto> payResp = restTemplate.exchange("/api/fees/payments", HttpMethod.POST,
                                new HttpEntity<>(payReq, headers), FeePaymentDto.class);
                Assertions.assertThat(payResp.getStatusCode()).isEqualTo(HttpStatus.OK);

                // 5. Ensure lateFeeApplied true and accrued is 500
                StudentFeeAssignment afterPayment = assignmentRepository.findById(assignment.getId()).get();
                Assertions.assertThat(afterPayment.getLateFeeAccrued()).isEqualByComparingTo(BigDecimal.valueOf(500));
                Assertions.assertThat(afterPayment.isLateFeeApplied()).isTrue();

                // 6. Another payment should NOT add more late fee
                payReq.setAllocations(List.of(
                                FeePaymentAllocationRequest.builder()
                                                .assignmentId(assignment.getId())
                                                .principalAmount(BigDecimal.valueOf(100))
                                                .build()));
                restTemplate.exchange("/api/fees/payments", HttpMethod.POST, new HttpEntity<>(payReq, headers),
                                FeePaymentDto.class);

                StudentFeeAssignment afterSecondPayment = assignmentRepository.findById(assignment.getId()).get();
                Assertions.assertThat(afterSecondPayment.getLateFeeAccrued())
                                .isEqualByComparingTo(BigDecimal.valueOf(500));
        }

        private void setupData() {
                Map<String, Object> schoolReq = Map.of("name", "Idem School", "displayName", "IS", "board", "CBSE",
                                "schoolCode", "IS-1", "city", "V", "state", "UP");
                ResponseEntity<School> schoolResp = restTemplate.exchange("/api/schools", HttpMethod.POST,
                                new HttpEntity<>(schoolReq, headers), School.class);
                schoolId = Objects.requireNonNull(schoolResp.getBody()).getId();
                loginAsSchoolAdmin(schoolId);
                sessionId = setupSession(schoolId, sessionRepository, schoolRepository);
                setSessionHeader(sessionId);

                // Class
                Map<String, Object> classReq = Map.of("name", "10", "sessionId", sessionId, "schoolId", schoolId);
                ResponseEntity<Map> classResp = restTemplate.exchange("/api/classes", HttpMethod.POST,
                                new HttpEntity<>(classReq, headers), Map.class);
                Long classId = Long.valueOf(classResp.getBody().get("id").toString());

                // Student
                StudentCreateRequest sreq = new StudentCreateRequest();
                sreq.setAdmissionNumber("I-1");
                sreq.setFirstName("I");
                sreq.setGender(Gender.MALE);
                sreq.setGuardians(List.of(GuardianCreateRequest.builder().name("G").contactNumber("1234567890")
                                .relation("FATHER").primaryGuardian(true).build()));
                studentId = restTemplate
                                .exchange("/api/students", HttpMethod.POST, new HttpEntity<>(sreq, headers),
                                                StudentDto.class)
                                .getBody()
                                .getId();

                // Fee Type
                FeeType typeReq = new FeeType();
                typeReq.setName("T");
                Long feeTypeId = restTemplate
                                .exchange("/api/fees/types", HttpMethod.POST, new HttpEntity<>(typeReq, headers),
                                                FeeType.class)
                                .getBody().getId();

                // Structure
                FeeStructureCreateRequest fsReq = new FeeStructureCreateRequest();
                fsReq.setClassId(classId);
                fsReq.setSessionId(sessionId);
                fsReq.setFeeTypeId(feeTypeId);
                fsReq.setAmount(BigDecimal.valueOf(5000));
                fsReq.setFrequency(FeeFrequency.ONE_TIME);
                Long feeStructureId = restTemplate.exchange("/api/fees/structures", HttpMethod.POST,
                                new HttpEntity<>(fsReq, headers), FeeStructureDto.class).getBody().getId();

                // Assign
                StudentFeeAssignRequest assignReq = new StudentFeeAssignRequest();
                assignReq.setStudentId(studentId);
                assignReq.setFeeStructureId(feeStructureId);
                assignReq.setSessionId(sessionId);
                restTemplate.exchange("/api/fees/assignments", HttpMethod.POST, new HttpEntity<>(assignReq, headers),
                                StudentFeeAssignmentDto.class);
        }

        @AfterEach
        void cleanup() {
                fullCleanup();
        }
}
