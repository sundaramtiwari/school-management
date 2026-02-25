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
import com.school.backend.fee.dto.StudentFeeAssignRequest;
import com.school.backend.fee.dto.StudentFeeAssignmentDto;
import com.school.backend.fee.dto.FeeStructureCreateRequest;
import com.school.backend.fee.dto.FeeStructureDto;
import com.school.backend.fee.entity.FeeType;
import com.school.backend.fee.entity.StudentFeeAssignment;
import com.school.backend.school.entity.School;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class OverpaymentRejectionTest extends BaseAuthenticatedIntegrationTest {

        private Long studentId;
        private Long sessionId;

        @Test
        void should_reject_overpayment() {
                setupData(BigDecimal.valueOf(10000));

                Long assignmentId = assignmentRepository.findByStudentIdAndSessionId(studentId, sessionId)
                                .get(0).getId();

                // Attempt to pay 10001
                FeePaymentRequest payReq = new FeePaymentRequest();
                payReq.setStudentId(studentId);
                payReq.setSessionId(sessionId);
                payReq.setAllocations(List.of(
                                FeePaymentAllocationRequest.builder()
                                                .assignmentId(assignmentId)
                                                .principalAmount(BigDecimal.valueOf(10001))
                                                .build()));
                payReq.setMode("CASH");

                ResponseEntity<Map> resp = restTemplate.exchange("/api/fees/payments", HttpMethod.POST,
                                new HttpEntity<>(payReq, headers), Map.class);

                // Expect failure (BusinessException usually maps to 400 or 500 depending on
                // handler)
                Assertions.assertThat(resp.getStatusCode()).isNotEqualTo(HttpStatus.OK);

                // Ensure no state mutation
                StudentFeeAssignment assignment = assignmentRepository.findByStudentIdAndSessionId(studentId, sessionId)
                                .get(0);
                Assertions.assertThat(assignment.getPrincipalPaid()).isEqualByComparingTo(BigDecimal.ZERO);

                Assertions.assertThat(feePaymentRepository.findByStudentId(studentId)).isEmpty();
        }

        private void setupData(BigDecimal feeAmount) {
                Map<String, Object> schoolReq = Map.of("name", "Overpay School", "displayName", "OS", "board", "CBSE",
                                "schoolCode", "OS-1", "city", "V", "state", "UP");
                ResponseEntity<School> schoolResp = restTemplate.exchange("/api/schools", HttpMethod.POST,
                                new HttpEntity<>(schoolReq, headers), School.class);
                Long schoolId = Objects.requireNonNull(schoolResp.getBody()).getId();
                loginAsSchoolAdmin(schoolId);
                sessionId = setupSession(schoolId, sessionRepository, schoolRepository);
                setSessionHeader(sessionId);

                // Class
                Map<String, Object> classReq = Map.of("name", "10", "sessionId", sessionId, "schoolId", schoolId);
                Long classId = Long.valueOf(
                                restTemplate.exchange("/api/classes", HttpMethod.POST,
                                                new HttpEntity<>(classReq, headers), Map.class)
                                                .getBody().get("id").toString());

                // Student
                StudentCreateRequest sreq = new StudentCreateRequest();
                sreq.setAdmissionNumber("O-1");
                sreq.setFirstName("O");
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
                fsReq.setAmount(feeAmount);
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
