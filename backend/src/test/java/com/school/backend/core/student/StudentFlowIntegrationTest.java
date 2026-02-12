package com.school.backend.core.student;

import com.school.backend.common.BaseAuthenticatedIntegrationTest;
import com.school.backend.common.dto.PageResponse;
import com.school.backend.common.enums.Gender;
import com.school.backend.core.student.dto.*;
import com.school.backend.fee.dto.*;
import com.school.backend.fee.entity.FeeType;
import com.school.backend.school.entity.School;
import org.assertj.core.api.Assertions;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.Objects;

public class StudentFlowIntegrationTest extends BaseAuthenticatedIntegrationTest {

    private Long schoolId;
    private Long classId;
    private Long studentId;
    private Long feeStructureId;
    private Long feeTypeId;
    private Long session2025Id;

    @Test
    void student_register_enroll_promote_flow() {

        // ---------- Create School ----------

        Map<String, Object> schoolReq = Map.of(
                "name", "Student Module School",
                "displayName", "SMS",
                "board", "CBSE",
                "schoolCode", "SMS-2025",
                "contactEmail", "sms@example.com",
                "city", "Varanasi",
                "state", "Uttar Pradesh");

        HttpEntity<Map<String, Object>> schoolEntity = new HttpEntity<>(schoolReq, headers);

        ResponseEntity<School> schoolResp = restTemplate.exchange(
                "/api/schools",
                HttpMethod.POST,
                schoolEntity,
                School.class);

        Assertions.assertThat(schoolResp.getStatusCode())
                .isEqualTo(HttpStatus.CREATED);

        schoolId = Objects.requireNonNull(schoolResp.getBody()).getId();

        // LOGIN AS SCHOOL ADMIN NOW
        loginAsSchoolAdmin(schoolId);

        // Create Session
        session2025Id = setupSession(schoolId, sessionRepository, schoolRepository);
        setSessionHeader(session2025Id);

        // ---------- Create Class ----------

        Map<String, Object> classReq = Map.of("name", "1",
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

        classId = Long.valueOf(
                String.valueOf(Objects.requireNonNull(classResp.getBody()).get("id")));

        // ---------- Register Student ----------

        StudentCreateRequest sreq = new StudentCreateRequest();

        sreq.setAdmissionNumber("ADM-100");
        sreq.setFirstName("Test");
        sreq.setGender(Gender.MALE);

        HttpEntity<StudentCreateRequest> studentEntity = new HttpEntity<>(sreq, headers);

        ResponseEntity<StudentDto> sResp = restTemplate.exchange(
                "/api/students",
                HttpMethod.POST,
                studentEntity,
                StudentDto.class);

        Assertions.assertThat(sResp.getStatusCode())
                .isEqualTo(HttpStatus.OK);

        studentId = Objects.requireNonNull(sResp.getBody()).getId();

        // ---------- Enroll Student ----------

        StudentEnrollmentRequest er = new StudentEnrollmentRequest();

        er.setStudentId(studentId);
        er.setClassId(classId);
        er.setSessionId(session2025Id);

        HttpEntity<StudentEnrollmentRequest> enrollEntity = new HttpEntity<>(er, headers);

        ResponseEntity<StudentEnrollmentDto> enrollResp = restTemplate.exchange(
                "/api/enrollments",
                HttpMethod.POST,
                enrollEntity,
                StudentEnrollmentDto.class);

        Assertions.assertThat(enrollResp.getStatusCode())
                .isEqualTo(HttpStatus.OK);

        // ---------- Fee Setup ----------

        FeeType type = new FeeType();
        type.setName("TUITION");

        HttpEntity<FeeType> typeEntity = new HttpEntity<>(type, headers);

        ResponseEntity<FeeType> typeResp = restTemplate.exchange(
                "/api/fees/types",
                HttpMethod.POST,
                typeEntity,
                FeeType.class);

        feeTypeId = Objects.requireNonNull(typeResp.getBody()).getId();

        FeeStructureCreateRequest fsReq = new FeeStructureCreateRequest();

        fsReq.setClassId(classId);
        fsReq.setSessionId(session2025Id);
        fsReq.setFeeTypeId(feeTypeId);
        fsReq.setAmount(10000);
        fsReq.setFrequency(com.school.backend.fee.enums.FeeFrequency.ONE_TIME);

        HttpEntity<FeeStructureCreateRequest> fsEntity = new HttpEntity<>(fsReq, headers);

        ResponseEntity<FeeStructureDto> fsResp = restTemplate.exchange(
                "/api/fees/structures",
                HttpMethod.POST,
                fsEntity,
                FeeStructureDto.class);

        feeStructureId = Objects.requireNonNull(fsResp.getBody()).getId();

        StudentFeeAssignRequest assignReq = new StudentFeeAssignRequest();

        assignReq.setStudentId(studentId);
        assignReq.setFeeStructureId(feeStructureId);
        assignReq.setSessionId(session2025Id);

        HttpEntity<StudentFeeAssignRequest> assignEntity = new HttpEntity<>(assignReq, headers);

        restTemplate.exchange(
                "/api/fees/assignments",
                HttpMethod.POST,
                assignEntity,
                StudentFeeAssignmentDto.class);

        FeePaymentRequest payReq = new FeePaymentRequest();

        payReq.setStudentId(studentId);
        payReq.setSessionId(session2025Id);
        payReq.setAmountPaid(10000);
        payReq.setMode("CASH");

        HttpEntity<FeePaymentRequest> payEntity = new HttpEntity<>(payReq, headers);

        restTemplate.exchange(
                "/api/fees/payments",
                HttpMethod.POST,
                payEntity,
                FeePaymentDto.class);

        // ---------- Promote ----------

        PromotionRequest pr = new PromotionRequest();

        pr.setToClassId(classId);
        pr.setToSection("B");
        pr.setSessionId(session2025Id);
        pr.setPromoted(true);
        pr.setFeePending(false);
        pr.setRemarks("Promoted successfully");

        HttpEntity<PromotionRequest> promoteEntity = new HttpEntity<>(pr, headers);

        ResponseEntity<PromotionRecordDto> promoteRes = restTemplate.exchange(
                "/api/students/" + studentId + "/history/promote",
                HttpMethod.POST,
                promoteEntity,
                PromotionRecordDto.class);

        Assertions.assertThat(promoteRes.getStatusCode())
                .isEqualTo(HttpStatus.OK);

        // ---------- List Enrollments ----------

        HttpEntity<Void> listEntity = new HttpEntity<>(headers);

        ResponseEntity<PageResponse<StudentEnrollmentDto>> pageResp = restTemplate.exchange(
                "/api/enrollments/by-class/" + classId + "?page=0&size=10",
                HttpMethod.GET,
                listEntity,
                new ParameterizedTypeReference<>() {
                });

        Assertions.assertThat(pageResp.getStatusCode())
                .isEqualTo(HttpStatus.OK);

        Assertions.assertThat(Objects.requireNonNull(pageResp.getBody()).content())
                .isNotEmpty();
    }

    // ------------------------------------------------

    @Test
    void student_update_profile_flow() {

        // ---------- Create School ----------

        Map<String, Object> schoolReq = Map.of(
                "name", "Update Test School",
                "displayName", "UTS",
                "board", "CBSE",
                "schoolCode", "UTS-2025",
                "contactEmail", "uts@example.com",
                "city", "Varanasi",
                "state", "Uttar Pradesh");

        HttpEntity<Map<String, Object>> schoolEntity = new HttpEntity<>(schoolReq, headers);

        ResponseEntity<School> schoolResp = restTemplate.exchange(
                "/api/schools",
                HttpMethod.POST,
                schoolEntity,
                School.class);

        Assertions.assertThat(schoolResp.getStatusCode())
                .isEqualTo(HttpStatus.CREATED);

        schoolId = Objects.requireNonNull(schoolResp.getBody()).getId();

        loginAsSchoolAdmin(schoolId);
        // ---------- Create Student ----------

        StudentCreateRequest createReq = new StudentCreateRequest();

        createReq.setAdmissionNumber("ADM-200");
        createReq.setFirstName("Rahul");
        createReq.setLastName("Sharma");
        createReq.setGender(Gender.MALE);

        HttpEntity<StudentCreateRequest> createEntity = new HttpEntity<>(createReq, headers);

        ResponseEntity<StudentDto> createResp = restTemplate.exchange(
                "/api/students",
                HttpMethod.POST,
                createEntity,
                StudentDto.class);

        Assertions.assertThat(createResp.getStatusCode())
                .isEqualTo(HttpStatus.OK);

        studentId = Objects.requireNonNull(createResp.getBody()).getId();

        // ---------- Update Student ----------

        HttpEntity<StudentUpdateRequest> updateEntity = getStudentUpdateRequestHttpEntity();

        ResponseEntity<StudentDto> updateResp = restTemplate.exchange(
                "/api/students/" + studentId,
                HttpMethod.PUT,
                updateEntity,
                StudentDto.class);

        Assertions.assertThat(updateResp.getStatusCode())
                .isEqualTo(HttpStatus.OK);

        // ---------- Fetch Student ----------

        HttpEntity<Void> getEntity = new HttpEntity<>(headers);

        ResponseEntity<StudentDto> getResp = restTemplate.exchange(
                "/api/students/" + studentId,
                HttpMethod.GET,
                getEntity,
                StudentDto.class);

        Assertions.assertThat(getResp.getStatusCode())
                .isEqualTo(HttpStatus.OK);

        StudentDto updated = Objects.requireNonNull(getResp.getBody());

        Assertions.assertThat(updated).isNotNull();

        // ---------- Assertions ----------

        Assertions.assertThat(updated.getFirstName())
                .isEqualTo("Rahul Updated");

        Assertions.assertThat(updated.getContactNumber())
                .isEqualTo("9999999999");

        Assertions.assertThat(updated.getCity())
                .isEqualTo("Lucknow");

        Assertions.assertThat(updated.getPreviousSchoolName())
                .isEqualTo("ABC Public School");

        Assertions.assertThat(updated.getPreviousSchoolBoard())
                .isEqualTo("ICSE");

        Assertions.assertThat(updated.getPreviousClass())
                .isEqualTo("5");

        Assertions.assertThat(updated.getPreviousYearOfPassing())
                .isEqualTo(2023);

        Assertions.assertThat(updated.getTransferCertificateNumber())
                .isEqualTo("TC-12345");
    }

    private @NonNull HttpEntity<StudentUpdateRequest> getStudentUpdateRequestHttpEntity() {
        StudentUpdateRequest updateReq = new StudentUpdateRequest();

        updateReq.setFirstName("Rahul Updated");
        updateReq.setContactNumber("9999999999");
        updateReq.setCity("Lucknow");

        updateReq.setPreviousSchoolName("ABC Public School");
        updateReq.setPreviousSchoolBoard("ICSE");
        updateReq.setPreviousClass("5");
        updateReq.setPreviousYearOfPassing(2023);
        updateReq.setTransferCertificateNumber("TC-12345");

        return new HttpEntity<>(updateReq, headers);
    }

    @AfterEach
    void cleanup() {
        fullCleanup();
    }
}
