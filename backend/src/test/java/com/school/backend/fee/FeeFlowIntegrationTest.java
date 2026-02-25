package com.school.backend.fee;

import com.school.backend.common.BaseAuthenticatedIntegrationTest;
import com.school.backend.common.enums.FeeFrequency;
import com.school.backend.common.enums.Gender;
import com.school.backend.core.guardian.dto.GuardianCreateRequest;
import com.school.backend.core.student.dto.StudentCreateRequest;
import com.school.backend.core.student.dto.StudentDto;
import com.school.backend.fee.dto.*;
import com.school.backend.fee.entity.FeeType;
import com.school.backend.school.entity.School;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class FeeFlowIntegrationTest extends BaseAuthenticatedIntegrationTest {

        private Long schoolId;
        private Long classId;
        private Long studentId;
        private Long feeTypeId;
        private Long feeStructureId;
        private Long session2025Id;

        @Test
        void full_fee_flow_should_work() {

                /* ----------------- 1. Create School ----------------- */

                Map<String, Object> schoolReq = Map.of(
                                "name", "Fee Test School",
                                "displayName", "FTS",
                                "board", "CBSE",
                                "schoolCode", "FTS-2026",
                                "city", "Varanasi",
                                "state", "UP");

                HttpEntity<Map<String, Object>> schoolEntity = new HttpEntity<>(schoolReq, headers);

                ResponseEntity<School> schoolResp = restTemplate.exchange(
                                "/api/schools",
                                HttpMethod.POST,
                                schoolEntity,
                                School.class);

                Assertions.assertThat(schoolResp.getStatusCode())
                                .isEqualTo(HttpStatus.CREATED);

                School schoolBody = schoolResp.getBody();
                Assertions.assertThat(schoolBody).isNotNull();
                schoolId = Objects.requireNonNull(schoolBody).getId();
                // LOGIN AS SCHOOL ADMIN NOW
                loginAsSchoolAdmin(schoolId);

                // Create Session
                session2025Id = setupSession(schoolId, sessionRepository, schoolRepository);
                setSessionHeader(session2025Id);

                /* ----------------- 2. Create Class ----------------- */

                Map<String, Object> classReq = Map.of(
                                "name", "5",
                                "sessionId", session2025Id,
                                "schoolId", schoolId);

                HttpEntity<Map<String, Object>> classEntity = new HttpEntity<>(classReq, headers);

                ResponseEntity<Map<String, Object>> classResp = restTemplate.exchange(
                                "/api/classes",
                                HttpMethod.POST,
                                classEntity,
                                new ParameterizedTypeReference<>() {
                                });

                Assertions.assertThat(classResp.getStatusCode())
                                .isEqualTo(HttpStatus.OK);

                Map<String, Object> classBody = Objects.requireNonNull(classResp.getBody());
                classId = Long.valueOf(
                                String.valueOf(classBody.get("id")));

                /* ----------------- 3. Create Student ----------------- */

                StudentCreateRequest sreq = new StudentCreateRequest();

                sreq.setAdmissionNumber("ADM-FEE-1");
                sreq.setFirstName("Fee");
                sreq.setGender(Gender.MALE);
                sreq.setGuardians(List.of(GuardianCreateRequest.builder()
                                .name("Fee Guardian")
                                .contactNumber("5544332211")
                                .relation("FATHER")
                                .primaryGuardian(true)
                                .build()));

                HttpEntity<StudentCreateRequest> studentEntity = new HttpEntity<>(sreq, headers);

                ResponseEntity<StudentDto> studentResp = restTemplate.exchange(
                                "/api/students",
                                HttpMethod.POST,
                                studentEntity,
                                StudentDto.class);

                Assertions.assertThat(studentResp.getStatusCode())
                                .isEqualTo(HttpStatus.OK);

                StudentDto studentBody = Objects.requireNonNull(studentResp.getBody());
                studentId = studentBody.getId();

                /* ----------------- 4. Create FeeType ----------------- */

                FeeType typeReq = new FeeType();
                typeReq.setName("TUITION");

                HttpEntity<FeeType> typeEntity = new HttpEntity<>(typeReq, headers);

                ResponseEntity<FeeType> typeResp = restTemplate.exchange(
                                "/api/fees/types",
                                HttpMethod.POST,
                                typeEntity,
                                FeeType.class);

                Assertions.assertThat(typeResp.getStatusCode())
                                .isEqualTo(HttpStatus.OK);

                FeeType typeBody = Objects.requireNonNull(typeResp.getBody());
                feeTypeId = typeBody.getId();

                /* ----------------- 5. Create FeeStructure ----------------- */

                FeeStructureCreateRequest fsReq = new FeeStructureCreateRequest();

                fsReq.setClassId(classId);
                fsReq.setSessionId(session2025Id);
                fsReq.setFeeTypeId(feeTypeId);
                fsReq.setAmount(java.math.BigDecimal.valueOf(12000));
                fsReq.setFrequency(FeeFrequency.ONE_TIME);

                HttpEntity<FeeStructureCreateRequest> fsEntity = new HttpEntity<>(fsReq, headers);

                ResponseEntity<FeeStructureDto> fsResp = restTemplate.exchange(
                                "/api/fees/structures",
                                HttpMethod.POST,
                                fsEntity,
                                FeeStructureDto.class);

                Assertions.assertThat(fsResp.getStatusCode())
                                .isEqualTo(HttpStatus.OK);

                FeeStructureDto fsBody = Objects.requireNonNull(fsResp.getBody());
                feeStructureId = fsBody.getId();

                /* ----------------- 6. Assign Fee ----------------- */

                StudentFeeAssignRequest assignReq = new StudentFeeAssignRequest();

                assignReq.setStudentId(studentId);
                assignReq.setFeeStructureId(feeStructureId);
                assignReq.setSessionId(session2025Id);

                HttpEntity<StudentFeeAssignRequest> assignEntity = new HttpEntity<>(assignReq, headers);

                ResponseEntity<StudentFeeAssignmentDto> assignResp = restTemplate.exchange(
                                "/api/fees/assignments",
                                HttpMethod.POST,
                                assignEntity,
                                StudentFeeAssignmentDto.class);

                Assertions.assertThat(assignResp.getStatusCode())
                                .isEqualTo(HttpStatus.OK);

                /* ----------------- 7. Pay Fee ----------------- */
                StudentFeeAssignmentDto assignment = Objects.requireNonNull(assignResp.getBody());

                FeePaymentRequest payReq = new FeePaymentRequest();

                payReq.setStudentId(studentId);
                payReq.setSessionId(session2025Id);
                payReq.setAllocations(List.of(
                                FeePaymentAllocationRequest.builder()
                                                .assignmentId(assignment.getId())
                                                .principalAmount(java.math.BigDecimal.valueOf(5000))
                                                .build()));
                payReq.setMode("UPI");

                HttpEntity<FeePaymentRequest> payEntity = new HttpEntity<>(payReq, headers);

                ResponseEntity<FeePaymentDto> payResp = restTemplate.exchange(
                                "/api/fees/payments",
                                HttpMethod.POST,
                                payEntity,
                                FeePaymentDto.class);

                Assertions.assertThat(payResp.getStatusCode())
                                .isEqualTo(HttpStatus.OK);

                /* ----------------- 8. Get Summary ----------------- */

                String url = "/api/fees/summary/students/" + studentId + "?sessionId=" + session2025Id;

                HttpEntity<Void> summaryEntity = new HttpEntity<>(headers);

                ResponseEntity<FeeSummaryDto> summaryResp = restTemplate.exchange(
                                url,
                                HttpMethod.GET,
                                summaryEntity,
                                FeeSummaryDto.class);

                Assertions.assertThat(summaryResp.getStatusCode())
                                .isEqualTo(HttpStatus.OK);

                FeeSummaryDto summary = Objects.requireNonNull(summaryResp.getBody());

                Assertions.assertThat(summary).isNotNull();
                Assertions.assertThat(summary.getTotalFee()).isEqualByComparingTo(java.math.BigDecimal.valueOf(12000));
                Assertions.assertThat(summary.getTotalPaid()).isEqualByComparingTo(java.math.BigDecimal.valueOf(5000));
                Assertions.assertThat(summary.getPendingFee()).isEqualByComparingTo(java.math.BigDecimal.valueOf(7000));
                Assertions.assertThat(summary.isFeePending()).isTrue();
        }

        @AfterEach
        void cleanup() {
                fullCleanup();
        }
}
