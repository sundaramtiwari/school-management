package com.school.backend.fee;

import com.school.backend.common.BaseAuthenticatedIntegrationTest;
import com.school.backend.common.enums.FeeFrequency;
import com.school.backend.common.enums.Gender;
import com.school.backend.core.guardian.dto.GuardianCreateRequest;
import com.school.backend.core.student.dto.StudentCreateRequest;
import com.school.backend.core.student.dto.StudentDto;
import com.school.backend.fee.dto.FeeStructureCreateRequest;
import com.school.backend.fee.dto.FeeStructureDto;
import com.school.backend.fee.dto.FeeSummaryDto;
import com.school.backend.fee.dto.LateFeeWaiverRequest;
import com.school.backend.fee.dto.StudentFeeAssignRequest;
import com.school.backend.fee.dto.StudentFeeAssignmentDto;
import com.school.backend.fee.entity.FeeAdjustment;
import com.school.backend.fee.entity.FeeType;
import com.school.backend.fee.entity.StudentFeeAssignment;
import com.school.backend.fee.repository.FeeAdjustmentRepository;
import com.school.backend.school.entity.School;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class LateFeeWaiverIntegrationTest extends BaseAuthenticatedIntegrationTest {

    @Autowired
    private FeeAdjustmentRepository feeAdjustmentRepository;

    private Long schoolId;
    private Long classId;
    private Long studentId;
    private Long feeTypeId;
    private Long feeStructureId;
    private Long assignmentId;
    private Long sessionId;

    @Test
    void waiveLateFee_should_create_adjustment_and_update_assignment() {
        setupBaseData();

        StudentFeeAssignment assignment = assignmentRepository.findById(assignmentId).orElseThrow();
        assignment.setLateFeeAccrued(new BigDecimal("100.00"));
        assignment.setLateFeePaid(new BigDecimal("20.00"));
        assignment.setLateFeeWaived(new BigDecimal("10.00"));
        assignmentRepository.save(assignment);

        LateFeeWaiverRequest req = new LateFeeWaiverRequest();
        req.setWaiverAmount(new BigDecimal("30.00"));
        req.setRemarks("Compassionate waiver");

        ResponseEntity<StudentFeeAssignmentDto> response = restTemplate.exchange(
                "/api/fees/assignments/" + assignmentId + "/waive-late-fee",
                HttpMethod.POST,
                new HttpEntity<>(req, headers),
                StudentFeeAssignmentDto.class);

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        StudentFeeAssignmentDto dto = Objects.requireNonNull(response.getBody());
        Assertions.assertThat(dto.getLateFeeWaived()).isEqualByComparingTo(new BigDecimal("40.00"));

        List<FeeAdjustment> adjustments = feeAdjustmentRepository.findByAssignmentId(assignmentId);
        Assertions.assertThat(adjustments).hasSize(1);
        FeeAdjustment adjustment = adjustments.get(0);
        Assertions.assertThat(adjustment.getType()).isEqualTo(FeeAdjustment.AdjustmentType.LATE_FEE_WAIVER);
        Assertions.assertThat(adjustment.getAmount()).isEqualByComparingTo(new BigDecimal("30.00"));
        Assertions.assertThat(adjustment.getReason()).isEqualTo("Compassionate waiver");
        Assertions.assertThat(adjustment.getCreatedByStaff()).isNotBlank();
    }

    @Test
    void waiveLateFee_should_reject_when_amount_exceeds_waivable_late_fee() {
        setupBaseData();

        StudentFeeAssignment assignment = assignmentRepository.findById(assignmentId).orElseThrow();
        assignment.setLateFeeAccrued(new BigDecimal("50.00"));
        assignment.setLateFeePaid(new BigDecimal("10.00"));
        assignment.setLateFeeWaived(new BigDecimal("5.00"));
        assignmentRepository.save(assignment);

        LateFeeWaiverRequest req = new LateFeeWaiverRequest();
        req.setWaiverAmount(new BigDecimal("36.00"));
        req.setRemarks("Invalid waiver");

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/fees/assignments/" + assignmentId + "/waive-late-fee",
                HttpMethod.POST,
                new HttpEntity<>(req, headers),
                Map.class);

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Assertions.assertThat(Objects.requireNonNull(response.getBody()))
                .containsEntry("message", "Waiver amount exceeds waivable late fee.");
    }

    @Test
    void summary_pending_should_subtract_waived_late_fee() {
        setupBaseData();

        StudentFeeAssignment assignment = assignmentRepository.findById(assignmentId).orElseThrow();
        assignment.setLateFeeAccrued(new BigDecimal("200.00"));
        assignment.setLateFeePaid(new BigDecimal("40.00"));
        assignment.setLateFeeWaived(new BigDecimal("50.00"));
        assignment.setPrincipalPaid(new BigDecimal("100.00"));
        assignmentRepository.save(assignment);

        ResponseEntity<FeeSummaryDto> response = restTemplate.exchange(
                "/api/fees/summary/students/" + studentId + "?sessionId=" + sessionId,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                FeeSummaryDto.class);

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        FeeSummaryDto summary = Objects.requireNonNull(response.getBody());
        Assertions.assertThat(summary.getPendingFee()).isEqualByComparingTo(new BigDecimal("1010.00"));
    }

    private void setupBaseData() {
        Map<String, Object> schoolReq = Map.of(
                "name", "Waiver Test School",
                "displayName", "WTS",
                "board", "CBSE",
                "schoolCode", "WTS-" + System.currentTimeMillis());
        ResponseEntity<School> schoolResp = restTemplate.exchange(
                "/api/schools",
                HttpMethod.POST,
                new HttpEntity<>(schoolReq, headers),
                School.class);
        schoolId = Objects.requireNonNull(schoolResp.getBody()).getId();
        loginAsSchoolAdmin(schoolId);

        sessionId = setupSession(schoolId, sessionRepository, schoolRepository);
        setSessionHeader(sessionId);

        Map<String, Object> classReq = Map.of("name", "9", "sessionId", sessionId, "schoolId", schoolId);
        ResponseEntity<Map> classResp = restTemplate.exchange(
                "/api/classes",
                HttpMethod.POST,
                new HttpEntity<>(classReq, headers),
                Map.class);
        classId = Long.valueOf(Objects.requireNonNull(classResp.getBody()).get("id").toString());

        StudentCreateRequest studentReq = new StudentCreateRequest();
        studentReq.setAdmissionNumber("WAIVE-" + System.currentTimeMillis());
        studentReq.setFirstName("Late");
        studentReq.setGender(Gender.MALE);
        studentReq.setGuardians(List.of(GuardianCreateRequest.builder()
                .name("Guardian")
                .contactNumber("9999999999")
                .relation("FATHER")
                .primaryGuardian(true)
                .build()));
        ResponseEntity<StudentDto> studentResp = restTemplate.exchange(
                "/api/students",
                HttpMethod.POST,
                new HttpEntity<>(studentReq, headers),
                StudentDto.class);
        studentId = Objects.requireNonNull(studentResp.getBody()).getId();

        FeeType feeTypeReq = new FeeType();
        feeTypeReq.setName("WAIVER_TEST_" + System.currentTimeMillis());
        ResponseEntity<FeeType> feeTypeResp = restTemplate.exchange(
                "/api/fees/types",
                HttpMethod.POST,
                new HttpEntity<>(feeTypeReq, headers),
                FeeType.class);
        feeTypeId = Objects.requireNonNull(feeTypeResp.getBody()).getId();

        FeeStructureCreateRequest structureReq = new FeeStructureCreateRequest();
        structureReq.setClassId(classId);
        structureReq.setSessionId(sessionId);
        structureReq.setFeeTypeId(feeTypeId);
        structureReq.setAmount(new BigDecimal("1000.00"));
        structureReq.setFrequency(FeeFrequency.ONE_TIME);
        ResponseEntity<FeeStructureDto> structureResp = restTemplate.exchange(
                "/api/fees/structures",
                HttpMethod.POST,
                new HttpEntity<>(structureReq, headers),
                FeeStructureDto.class);
        feeStructureId = Objects.requireNonNull(structureResp.getBody()).getId();

        StudentFeeAssignRequest assignReq = new StudentFeeAssignRequest();
        assignReq.setStudentId(studentId);
        assignReq.setFeeStructureId(feeStructureId);
        assignReq.setSessionId(sessionId);
        ResponseEntity<StudentFeeAssignmentDto> assignResp = restTemplate.exchange(
                "/api/fees/assignments",
                HttpMethod.POST,
                new HttpEntity<>(assignReq, headers),
                StudentFeeAssignmentDto.class);
        assignmentId = Objects.requireNonNull(assignResp.getBody()).getId();
    }

    @AfterEach
    void cleanup() {
        feeAdjustmentRepository.deleteAll();
        fullCleanup();
    }
}
