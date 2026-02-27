package com.school.backend.fee;

import com.school.backend.common.BaseAuthenticatedIntegrationTest;
import com.school.backend.common.enums.DiscountType;
import com.school.backend.common.enums.FeeFrequency;
import com.school.backend.common.enums.Gender;
import com.school.backend.core.guardian.dto.GuardianCreateRequest;
import com.school.backend.core.student.dto.StudentCreateRequest;
import com.school.backend.core.student.dto.StudentDto;
import com.school.backend.fee.dto.FeeDiscountApplyRequest;
import com.school.backend.fee.dto.FeeAdjustmentDto;
import com.school.backend.fee.dto.FeeStructureCreateRequest;
import com.school.backend.fee.dto.FeeStructureDto;
import com.school.backend.fee.dto.StudentFeeAssignRequest;
import com.school.backend.fee.dto.StudentFeeAssignmentDto;
import com.school.backend.fee.entity.DiscountDefinition;
import com.school.backend.fee.entity.FeeAdjustment;
import com.school.backend.fee.entity.FeeType;
import com.school.backend.fee.repository.DiscountDefinitionRepository;
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

public class FeeDiscountIntegrationTest extends BaseAuthenticatedIntegrationTest {

    @Autowired
    private DiscountDefinitionRepository discountDefinitionRepository;
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
    void applyDiscount_should_create_adjustment_and_update_assignment_summary() {
        setupBaseData();

        DiscountDefinition definition = discountDefinitionRepository.save(DiscountDefinition.builder()
                .name("Merit 10")
                .type(DiscountType.PERCENTAGE)
                .amountValue(new BigDecimal("10.00"))
                .active(true)
                .schoolId(schoolId)
                .build());

        FeeDiscountApplyRequest req = new FeeDiscountApplyRequest();
        req.setDiscountDefinitionId(definition.getId());
        req.setRemarks("Scholarship");

        ResponseEntity<StudentFeeAssignmentDto> response = restTemplate.exchange(
                "/api/fees/assignments/" + assignmentId + "/discount",
                HttpMethod.POST,
                new HttpEntity<>(req, headers),
                StudentFeeAssignmentDto.class);

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        StudentFeeAssignmentDto dto = Objects.requireNonNull(response.getBody());
        Assertions.assertThat(dto.getAmount()).isEqualByComparingTo(new BigDecimal("1000.00"));
        Assertions.assertThat(dto.getPrincipalPaid()).isEqualByComparingTo(BigDecimal.ZERO);
        Assertions.assertThat(dto.getTotalDiscountAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        Assertions.assertThat(dto.getSponsorCoveredAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        Assertions.assertThat(dto.getLateFeeAccrued()).isEqualByComparingTo(BigDecimal.ZERO);
        Assertions.assertThat(dto.getPendingTillDate()).isEqualByComparingTo(new BigDecimal("900.00"));

        List<FeeAdjustment> adjustments = feeAdjustmentRepository.findByAssignmentId(assignmentId);
        Assertions.assertThat(adjustments).hasSize(1);
        FeeAdjustment adjustment = adjustments.get(0);
        Assertions.assertThat(adjustment.getType()).isEqualTo(FeeAdjustment.AdjustmentType.DISCOUNT);
        Assertions.assertThat(adjustment.getAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        Assertions.assertThat(adjustment.getReason()).isEqualTo("Scholarship");
        Assertions.assertThat(adjustment.getCreatedByStaff()).isNotBlank();
        Assertions.assertThat(adjustment.getDiscountDefinitionId()).isEqualTo(definition.getId());
        Assertions.assertThat(adjustment.getDiscountNameSnapshot()).isEqualTo("Merit 10");
        Assertions.assertThat(adjustment.getDiscountTypeSnapshot()).isEqualTo(DiscountType.PERCENTAGE);
        Assertions.assertThat(adjustment.getDiscountValueSnapshot()).isEqualByComparingTo(new BigDecimal("10.00"));
    }

    @Test
    void applyDiscount_should_reject_when_discount_exceeds_remaining_principal() {
        setupBaseData();

        DiscountDefinition first = discountDefinitionRepository.save(DiscountDefinition.builder()
                .name("Flat 800")
                .type(DiscountType.FLAT)
                .amountValue(new BigDecimal("800.00"))
                .active(true)
                .schoolId(schoolId)
                .build());
        DiscountDefinition second = discountDefinitionRepository.save(DiscountDefinition.builder()
                .name("Flat 300")
                .type(DiscountType.FLAT)
                .amountValue(new BigDecimal("300.00"))
                .active(true)
                .schoolId(schoolId)
                .build());

        FeeDiscountApplyRequest firstReq = new FeeDiscountApplyRequest();
        firstReq.setDiscountDefinitionId(first.getId());
        ResponseEntity<StudentFeeAssignmentDto> firstResp = restTemplate.exchange(
                "/api/fees/assignments/" + assignmentId + "/discount",
                HttpMethod.POST,
                new HttpEntity<>(firstReq, headers),
                StudentFeeAssignmentDto.class);
        Assertions.assertThat(firstResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        FeeDiscountApplyRequest secondReq = new FeeDiscountApplyRequest();
        secondReq.setDiscountDefinitionId(second.getId());
        ResponseEntity<Map> secondResp = restTemplate.exchange(
                "/api/fees/assignments/" + assignmentId + "/discount",
                HttpMethod.POST,
                new HttpEntity<>(secondReq, headers),
                Map.class);
        Assertions.assertThat(secondResp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Assertions.assertThat(secondResp.getBody()).containsEntry("message",
                "Calculated discount exceeds remaining principal due.");

        BigDecimal currentDiscount = assignmentRepository.findById(assignmentId)
                .orElseThrow()
                .getTotalDiscountAmount();
        Assertions.assertThat(currentDiscount).isEqualByComparingTo(new BigDecimal("800.00"));
        Assertions.assertThat(feeAdjustmentRepository.findByAssignmentId(assignmentId)).hasSize(1);
    }

    @Test
    void adjustmentHistory_should_use_snapshot_and_fallback_legacy_discount() {
        setupBaseData();

        feeAdjustmentRepository.save(FeeAdjustment.builder()
                .assignmentId(assignmentId)
                .type(FeeAdjustment.AdjustmentType.DISCOUNT)
                .amount(new BigDecimal("5.00"))
                .reason("Old legacy row")
                .createdByStaff(null)
                .schoolId(schoolId)
                .build());

        DiscountDefinition definition = discountDefinitionRepository.save(DiscountDefinition.builder()
                .name("Merit 25")
                .type(DiscountType.PERCENTAGE)
                .amountValue(new BigDecimal("25.00"))
                .active(true)
                .schoolId(schoolId)
                .build());

        FeeDiscountApplyRequest req = new FeeDiscountApplyRequest();
        req.setDiscountDefinitionId(definition.getId());
        req.setRemarks("Manual approval");
        ResponseEntity<StudentFeeAssignmentDto> applyResp = restTemplate.exchange(
                "/api/fees/assignments/" + assignmentId + "/discount",
                HttpMethod.POST,
                new HttpEntity<>(req, headers),
                StudentFeeAssignmentDto.class);
        Assertions.assertThat(applyResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<FeeAdjustmentDto[]> historyResp = restTemplate.exchange(
                "/api/fees/assignments/" + assignmentId + "/adjustments",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                FeeAdjustmentDto[].class);
        Assertions.assertThat(historyResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        FeeAdjustmentDto[] rows = Objects.requireNonNull(historyResp.getBody());
        Assertions.assertThat(rows).hasSize(2);
        Assertions.assertThat(rows[0].getDiscountName()).isEqualTo("Legacy Discount");
        Assertions.assertThat(rows[0].getType()).isEqualTo(FeeAdjustment.AdjustmentType.DISCOUNT);
        Assertions.assertThat(rows[1].getDiscountName()).isEqualTo("Merit 25");
        Assertions.assertThat(rows[1].getType()).isEqualTo(FeeAdjustment.AdjustmentType.DISCOUNT);
    }

    private void setupBaseData() {
        Map<String, Object> schoolReq = Map.of(
                "name", "Discount Test School",
                "displayName", "DTS",
                "board", "CBSE",
                "schoolCode", "DTS-" + System.currentTimeMillis());
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
        studentReq.setAdmissionNumber("DIS-" + System.currentTimeMillis());
        studentReq.setFirstName("Fee");
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
        feeTypeReq.setName("DISCOUNT_TEST_" + System.currentTimeMillis());
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
        fullCleanup();
    }
}
