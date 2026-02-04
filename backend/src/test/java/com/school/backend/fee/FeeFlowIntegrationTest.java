package com.school.backend.fee;

import com.school.backend.common.BaseAuthenticatedIntegrationTest;
import com.school.backend.common.enums.Gender;
import com.school.backend.core.classsubject.repository.SchoolClassRepository;
import com.school.backend.core.student.dto.StudentCreateRequest;
import com.school.backend.core.student.dto.StudentDto;
import com.school.backend.core.student.repository.StudentRepository;
import com.school.backend.fee.dto.*;
import com.school.backend.fee.entity.FeeType;
import com.school.backend.fee.repository.FeePaymentRepository;
import com.school.backend.fee.repository.FeeStructureRepository;
import com.school.backend.fee.repository.FeeTypeRepository;
import com.school.backend.fee.repository.StudentFeeAssignmentRepository;
import com.school.backend.school.entity.School;
import com.school.backend.school.repository.SchoolRepository;
import com.school.backend.user.repository.UserRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

public class FeeFlowIntegrationTest extends BaseAuthenticatedIntegrationTest {
    @Autowired
    private FeeTypeRepository feeTypeRepository;
    @Autowired
    private FeeStructureRepository feeStructureRepository;
    @Autowired
    private StudentFeeAssignmentRepository assignmentRepository;
    @Autowired
    private FeePaymentRepository paymentRepository;
    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private SchoolClassRepository classRepository;
    @Autowired
    private SchoolRepository schoolRepository;
    @Autowired
    private UserRepository userRepository;

    private Long schoolId;
    private Long classId;
    private Long studentId;
    private Long feeTypeId;
    private Long feeStructureId;

    @Test
    void full_fee_flow_should_work() {

        /* ----------------- 1. Create School ----------------- */

        Map<String, Object> schoolReq = Map.of(
                "name", "Fee Test School",
                "displayName", "FTS",
                "board", "CBSE",
                "schoolCode", "FTS-2026",
                "city", "Varanasi",
                "state", "UP"
        );

        HttpEntity<Map<String, Object>> schoolEntity =
                new HttpEntity<>(schoolReq, headers);

        ResponseEntity<School> schoolResp =
                restTemplate.exchange(
                        "/api/schools",
                        HttpMethod.POST,
                        schoolEntity,
                        School.class
                );

        Assertions.assertThat(schoolResp.getStatusCode())
                .isEqualTo(HttpStatus.CREATED);

        schoolId = schoolResp.getBody().getId();
        // LOGIN AS SCHOOL ADMIN NOW
        loginAsSchoolAdmin(schoolId);

        /* ----------------- 2. Create Class ----------------- */

        Map<String, Object> classReq = Map.of(
                "name", "5",
                "session", "2025-26",
                "schoolId", schoolId
        );

        HttpEntity<Map<String, Object>> classEntity =
                new HttpEntity<>(classReq, headers);

        ResponseEntity<Map> classResp =
                restTemplate.exchange(
                        "/api/classes",
                        HttpMethod.POST,
                        classEntity,
                        Map.class
                );

        Assertions.assertThat(classResp.getStatusCode())
                .isEqualTo(HttpStatus.OK);

        classId = Long.valueOf(
                String.valueOf(classResp.getBody().get("id"))
        );


        /* ----------------- 3. Create Student ----------------- */

        StudentCreateRequest sreq =
                new StudentCreateRequest();

        sreq.setAdmissionNumber("ADM-FEE-1");
        sreq.setFirstName("Fee");
        sreq.setGender(Gender.MALE);

        HttpEntity<StudentCreateRequest> studentEntity =
                new HttpEntity<>(sreq, headers);

        ResponseEntity<StudentDto> studentResp =
                restTemplate.exchange(
                        "/api/students",
                        HttpMethod.POST,
                        studentEntity,
                        StudentDto.class
                );

        Assertions.assertThat(studentResp.getStatusCode())
                .isEqualTo(HttpStatus.OK);

        studentId = studentResp.getBody().getId();


        /* ----------------- 4. Create FeeType ----------------- */

        FeeType typeReq = new FeeType();
        typeReq.setName("TUITION");

        HttpEntity<FeeType> typeEntity =
                new HttpEntity<>(typeReq, headers);

        ResponseEntity<FeeType> typeResp =
                restTemplate.exchange(
                        "/api/fees/types",
                        HttpMethod.POST,
                        typeEntity,
                        FeeType.class
                );

        Assertions.assertThat(typeResp.getStatusCode())
                .isEqualTo(HttpStatus.OK);

        feeTypeId = typeResp.getBody().getId();


        /* ----------------- 5. Create FeeStructure ----------------- */

        FeeStructureCreateRequest fsReq =
                new FeeStructureCreateRequest();

        fsReq.setClassId(classId);
        fsReq.setSession("2025-26");
        fsReq.setFeeTypeId(feeTypeId);
        fsReq.setAmount(12000);

        HttpEntity<FeeStructureCreateRequest> fsEntity =
                new HttpEntity<>(fsReq, headers);

        ResponseEntity<FeeStructureDto> fsResp =
                restTemplate.exchange(
                        "/api/fees/structures",
                        HttpMethod.POST,
                        fsEntity,
                        FeeStructureDto.class
                );

        Assertions.assertThat(fsResp.getStatusCode())
                .isEqualTo(HttpStatus.OK);

        feeStructureId = fsResp.getBody().getId();


        /* ----------------- 6. Assign Fee ----------------- */

        StudentFeeAssignRequest assignReq =
                new StudentFeeAssignRequest();

        assignReq.setStudentId(studentId);
        assignReq.setFeeStructureId(feeStructureId);
        assignReq.setSession("2025-26");

        HttpEntity<StudentFeeAssignRequest> assignEntity =
                new HttpEntity<>(assignReq, headers);

        ResponseEntity<StudentFeeAssignmentDto> assignResp =
                restTemplate.exchange(
                        "/api/fees/assignments",
                        HttpMethod.POST,
                        assignEntity,
                        StudentFeeAssignmentDto.class
                );

        Assertions.assertThat(assignResp.getStatusCode())
                .isEqualTo(HttpStatus.OK);


        /* ----------------- 7. Pay Fee ----------------- */

        FeePaymentRequest payReq =
                new FeePaymentRequest();

        payReq.setStudentId(studentId);
        payReq.setAmountPaid(5000);
        payReq.setMode("UPI");

        HttpEntity<FeePaymentRequest> payEntity =
                new HttpEntity<>(payReq, headers);

        ResponseEntity<FeePaymentDto> payResp =
                restTemplate.exchange(
                        "/api/fees/payments",
                        HttpMethod.POST,
                        payEntity,
                        FeePaymentDto.class
                );

        Assertions.assertThat(payResp.getStatusCode())
                .isEqualTo(HttpStatus.OK);


        /* ----------------- 8. Get Summary ----------------- */

        String url =
                "/api/fees/summary/students/" + studentId + "?session=2025-26";

        HttpEntity<Void> summaryEntity =
                new HttpEntity<>(headers);

        ResponseEntity<FeeSummaryDto> summaryResp =
                restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        summaryEntity,
                        FeeSummaryDto.class
                );

        Assertions.assertThat(summaryResp.getStatusCode())
                .isEqualTo(HttpStatus.OK);

        FeeSummaryDto summary = summaryResp.getBody();

        Assertions.assertThat(summary).isNotNull();
        Assertions.assertThat(summary.getTotalFee()).isEqualTo(12000);
        Assertions.assertThat(summary.getTotalPaid()).isEqualTo(5000);
        Assertions.assertThat(summary.getPendingFee()).isEqualTo(7000);
        Assertions.assertThat(summary.isFeePending()).isTrue();
    }

    // ------------------------------------------------

    @AfterEach
    void cleanup() {

        if (studentId != null) {

            paymentRepository.findByStudentId(studentId)
                    .forEach(paymentRepository::delete);

            assignmentRepository
                    .findByStudentIdAndSession(studentId, "2025-26")
                    .forEach(assignmentRepository::delete);

            studentRepository.deleteById(studentId);
        }

        if (feeStructureId != null) {
            feeStructureRepository.deleteById(feeStructureId);
        }

        if (feeTypeId != null) {
            feeTypeRepository.deleteById(feeTypeId);
        }

        if (classId != null) {
            classRepository.deleteById(classId);
        }

        if (schoolId != null) {
            userRepository
                    .findAll()
                    .stream()
                    .filter(u ->
                            u.getSchool() != null &&
                                    u.getSchool().getId().equals(schoolId)
                    )
                    .forEach(userRepository::delete);
        }

        if (schoolId != null) {
            schoolRepository.deleteById(schoolId);
        }
    }
}
