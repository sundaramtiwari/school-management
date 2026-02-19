package com.school.backend.fee;

import com.school.backend.common.BaseAuthenticatedIntegrationTest;
import com.school.backend.common.enums.FeeFrequency;
import com.school.backend.common.enums.Gender;
import com.school.backend.core.guardian.dto.GuardianCreateRequest;
import com.school.backend.core.student.dto.StudentCreateRequest;
import com.school.backend.core.student.dto.StudentDto;
import com.school.backend.fee.dto.*;
import com.school.backend.fee.entity.FeeType;
import com.school.backend.school.entity.AcademicSession;
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

public class FeeMultiSessionIntegrationTest extends BaseAuthenticatedIntegrationTest {

    private Long schoolId;
    private Long studentId;
    private Long feeTypeId;

    @Test
    void multi_session_ledger_should_be_accurate() {
        setupBaseData();

        // Session 1: 2023-24
        Long s1Id = createSession("2023-24", 2023);
        Long class1Id = createClass("1", s1Id);
        Long fs1Id = createFeeStructure(class1Id, s1Id, BigDecimal.valueOf(10000));
        assignFee(studentId, fs1Id, s1Id);
        payFee(studentId, s1Id, BigDecimal.valueOf(4000)); // 6000 pending

        // Session 2: 2024-25
        Long s2Id = createSession("2024-25", 2024);
        Long class2Id = createClass("2", s2Id);
        Long fs2Id = createFeeStructure(class2Id, s2Id, BigDecimal.valueOf(12000));
        assignFee(studentId, fs2Id, s2Id);
        payFee(studentId, s2Id, BigDecimal.valueOf(12000)); // 0 pending

        // Session 3: 2025-26
        Long s3Id = createSession("2025-26", 2025);
        Long class3Id = createClass("3", s3Id);
        Long fs3Id = createFeeStructure(class3Id, s3Id, BigDecimal.valueOf(15000));
        assignFee(studentId, fs3Id, s3Id);
        payFee(studentId, s3Id, BigDecimal.valueOf(5000)); // 10000 pending

        // Re-validate summaries for each session
        validateSummary(studentId, s1Id, BigDecimal.valueOf(10000), BigDecimal.valueOf(4000),
                BigDecimal.valueOf(6000));
        validateSummary(studentId, s2Id, BigDecimal.valueOf(12000), BigDecimal.valueOf(12000),
                BigDecimal.valueOf(0));
        validateSummary(studentId, s3Id, BigDecimal.valueOf(15000), BigDecimal.valueOf(5000),
                BigDecimal.valueOf(10000));

        // 2. Validate Grand Ledger
        validateLedger(studentId, BigDecimal.valueOf(37000), BigDecimal.valueOf(21000),
                BigDecimal.valueOf(16000));
    }

    private void setupBaseData() {
        Map<String, Object> schoolReq = Map.of("name", "Multi Session School", "displayName", "MSS", "board",
                "CBSE", "schoolCode", "MSS-1", "city", "V", "state", "UP");
        HttpEntity<Map<String, Object>> schoolEntity = new HttpEntity<>(schoolReq, headers);
        ResponseEntity<School> schoolResp = restTemplate.exchange("/api/schools", HttpMethod.POST, schoolEntity,
                School.class);
        schoolId = Objects.requireNonNull(schoolResp.getBody()).getId();
        loginAsSchoolAdmin(schoolId);

        // We need at least one session and one class to register a student
        Long initSessionId = createSession("Initial", 2022);
        createClass("InitialClass", initSessionId);

        // Student
        StudentCreateRequest sreq = new StudentCreateRequest();
        sreq.setAdmissionNumber("MS-1");
        sreq.setFirstName("Multi");
        sreq.setGender(Gender.FEMALE);
        sreq.setGuardians(List.of(GuardianCreateRequest.builder().name("G").contactNumber("1234567890")
                .relation("MOTHER").primaryGuardian(true).build()));
        HttpEntity<StudentCreateRequest> studentEntity = new HttpEntity<>(sreq, headers);
        ResponseEntity<StudentDto> studentResp = restTemplate.exchange("/api/students", HttpMethod.POST,
                studentEntity, StudentDto.class);
        Assertions.assertThat(studentResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        studentId = studentResp.getBody().getId();

        // Fee Type
        FeeType typeReq = new FeeType();
        typeReq.setName("TUITION");
        feeTypeId = restTemplate.exchange("/api/fees/types", HttpMethod.POST,
                new HttpEntity<>(typeReq, headers), FeeType.class).getBody().getId();
    }

    private Long createSession(String name, int startYear) {
        AcademicSession session = sessionRepository.save(AcademicSession.builder()
                .name(name)
                .startDate(LocalDate.of(startYear, 4, 1))
                .endDate(LocalDate.of(startYear + 1, 3, 31))
                .schoolId(schoolId)
                .active(true)
                .build());

        School school = schoolRepository.findById(schoolId).orElseThrow();
        school.setCurrentSessionId(session.getId());
        schoolRepository.save(school);

        return session.getId();
    }

    private Long createClass(String name, Long sessionId) {
        Map<String, Object> req = Map.of("name", name, "sessionId", sessionId, "schoolId", schoolId);
        setSessionHeader(sessionId);
        ResponseEntity<Map> resp = restTemplate.exchange("/api/classes", HttpMethod.POST,
                new HttpEntity<>(req, headers), Map.class);
        Assertions.assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        return Long.valueOf(resp.getBody().get("id").toString());
    }

    private Long createFeeStructure(Long classId, Long sessionId, BigDecimal amount) {
        FeeStructureCreateRequest req = new FeeStructureCreateRequest();
        req.setClassId(classId);
        req.setSessionId(sessionId);
        req.setFeeTypeId(feeTypeId);
        req.setAmount(amount);
        req.setFrequency(FeeFrequency.ONE_TIME);
        setSessionHeader(sessionId);
        ResponseEntity<FeeStructureDto> resp = restTemplate.exchange("/api/fees/structures", HttpMethod.POST,
                new HttpEntity<>(req, headers), FeeStructureDto.class);
        Assertions.assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        return resp.getBody().getId();
    }

    private void assignFee(Long studentId, Long fsId, Long sessionId) {
        StudentFeeAssignRequest req = new StudentFeeAssignRequest();
        req.setStudentId(studentId);
        req.setFeeStructureId(fsId);
        req.setSessionId(sessionId);
        setSessionHeader(sessionId);
        ResponseEntity<StudentFeeAssignmentDto> resp = restTemplate.exchange("/api/fees/assignments",
                HttpMethod.POST, new HttpEntity<>(req, headers), StudentFeeAssignmentDto.class);
        Assertions.assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private void payFee(Long studentId, Long sessionId, BigDecimal amount) {
        FeePaymentRequest req = new FeePaymentRequest();
        req.setStudentId(studentId);
        req.setSessionId(sessionId);
        req.setAmountPaid(amount);
        req.setMode("CASH");
        setSessionHeader(sessionId);
        ResponseEntity<FeePaymentDto> resp = restTemplate.exchange("/api/fees/payments", HttpMethod.POST,
                new HttpEntity<>(req, headers), FeePaymentDto.class);
        Assertions.assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private void validateSummary(Long studentId, Long sessionId, BigDecimal total, BigDecimal paid,
                                 BigDecimal pending) {
        setSessionHeader(sessionId);
        String url = "/api/fees/summary/students/" + studentId + "?sessionId=" + sessionId;
        ResponseEntity<FeeSummaryDto> resp = restTemplate.exchange(url, HttpMethod.GET,
                new HttpEntity<>(headers), FeeSummaryDto.class);

        Assertions.assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        FeeSummaryDto summary = resp.getBody();
        Assertions.assertThat(summary).isNotNull();
        Assertions.assertThat(summary.getTotalFee()).as("Total Fee Match for session " + sessionId)
                .isEqualByComparingTo(total);
        Assertions.assertThat(summary.getTotalPaid()).as("Total Paid Match").isEqualByComparingTo(paid);
        Assertions.assertThat(summary.getPendingFee()).as("Pending Fee Match").isEqualByComparingTo(pending);
    }

    private void validateLedger(Long studentId, BigDecimal grandTotal, BigDecimal grandPaid,
                                BigDecimal grandPending) {
        String url = "/api/fees/summary/students/" + studentId + "/ledger";
        ResponseEntity<StudentLedgerDto> resp = restTemplate.exchange(url, HttpMethod.GET,
                new HttpEntity<>(headers), StudentLedgerDto.class);

        Assertions.assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        StudentLedgerDto ledger = resp.getBody();
        Assertions.assertThat(ledger).isNotNull();
        Assertions.assertThat(ledger.getGrandTotalFee()).as("Grand Total Fee Match")
                .isEqualByComparingTo(grandTotal);
        Assertions.assertThat(ledger.getGrandTotalPaid()).as("Grand Total Paid Match")
                .isEqualByComparingTo(grandPaid);
        Assertions.assertThat(ledger.getGrandTotalPending()).as("Grand Total Pending Match")
                .isEqualByComparingTo(grandPending);
        Assertions.assertThat(ledger.getSessionSummaries()).hasSize(3); // s1, s2, s3 (Initial ignored because
        // total=0)
    }

    @AfterEach
    void cleanup() {
        fullCleanup();
    }
}
