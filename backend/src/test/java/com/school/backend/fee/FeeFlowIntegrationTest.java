package com.school.backend.fee;

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
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class FeeFlowIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

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

        ResponseEntity<School> schoolResp =
                restTemplate.postForEntity("/api/schools", schoolReq, School.class);

        Assertions.assertThat(schoolResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        schoolId = schoolResp.getBody().getId();


        /* ----------------- 2. Create Class ----------------- */

        Map<String, Object> classReq = Map.of(
                "name", "5",
                "session", "2025-26",
                "schoolId", schoolId
        );

        ResponseEntity<Map> classResp =
                restTemplate.postForEntity("/api/classes", classReq, Map.class);

        Assertions.assertThat(classResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        classId = Long.valueOf(String.valueOf(classResp.getBody().get("id")));


        /* ----------------- 3. Create Student ----------------- */

        StudentCreateRequest sreq = new StudentCreateRequest();

        sreq.setAdmissionNumber("ADM-FEE-1");
        sreq.setFirstName("Fee");
        sreq.setGender("Male");
        sreq.setSchoolId(schoolId);

        ResponseEntity<StudentDto> studentResp =
                restTemplate.postForEntity("/api/students", sreq, StudentDto.class);

        Assertions.assertThat(studentResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        studentId = studentResp.getBody().getId();


        /* ----------------- 4. Create FeeType ----------------- */

        FeeType typeReq = new FeeType();
        typeReq.setName("TUITION");

        ResponseEntity<FeeType> typeResp =
                restTemplate.postForEntity("/api/fees/types", typeReq, FeeType.class);

        Assertions.assertThat(typeResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        feeTypeId = typeResp.getBody().getId();


        /* ----------------- 5. Create FeeStructure ----------------- */

        FeeStructureCreateRequest fsReq = new FeeStructureCreateRequest();

        fsReq.setSchoolId(schoolId);
        fsReq.setClassId(classId);
        fsReq.setSession("2025-26");
        fsReq.setFeeTypeId(feeTypeId);
        fsReq.setAmount(12000);

        ResponseEntity<FeeStructureDto> fsResp =
                restTemplate.postForEntity("/api/fees/structures", fsReq, FeeStructureDto.class);

        Assertions.assertThat(fsResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        feeStructureId = fsResp.getBody().getId();


        /* ----------------- 6. Assign Fee ----------------- */

        StudentFeeAssignRequest assignReq = new StudentFeeAssignRequest();

        assignReq.setStudentId(studentId);
        assignReq.setFeeStructureId(feeStructureId);
        assignReq.setSession("2025-26");

        ResponseEntity<StudentFeeAssignmentDto> assignResp =
                restTemplate.postForEntity("/api/fees/assignments", assignReq,
                        StudentFeeAssignmentDto.class);

        Assertions.assertThat(assignResp.getStatusCode()).isEqualTo(HttpStatus.OK);


        /* ----------------- 7. Pay Fee ----------------- */

        FeePaymentRequest payReq = new FeePaymentRequest();

        payReq.setStudentId(studentId);
        payReq.setAmountPaid(5000);
        payReq.setMode("UPI");

        ResponseEntity<FeePaymentDto> payResp =
                restTemplate.postForEntity("/api/fees/payments", payReq,
                        FeePaymentDto.class);

        Assertions.assertThat(payResp.getStatusCode()).isEqualTo(HttpStatus.OK);


        /* ----------------- 8. Get Summary ----------------- */

        String url =
                "/api/fees/summary/students/" + studentId + "?session=2025-26";

        ResponseEntity<FeeSummaryDto> summaryResp =
                restTemplate.getForEntity(url, FeeSummaryDto.class);

        Assertions.assertThat(summaryResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        FeeSummaryDto summary = summaryResp.getBody();

        Assertions.assertThat(summary).isNotNull();
        Assertions.assertThat(summary.getTotalFee()).isEqualTo(12000);
        Assertions.assertThat(summary.getTotalPaid()).isEqualTo(5000);
        Assertions.assertThat(summary.getPendingFee()).isEqualTo(7000);
        Assertions.assertThat(summary.isFeePending()).isTrue();
    }


    /* ----------------- CLEANUP ----------------- */

    @AfterEach
    void cleanup() {

        if (studentId != null) {
            paymentRepository.findByStudentId(studentId)
                    .forEach(paymentRepository::delete);

            assignmentRepository.findByStudentIdAndSession(studentId, "2025-26")
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
            schoolRepository.deleteById(schoolId);
        }
    }
}