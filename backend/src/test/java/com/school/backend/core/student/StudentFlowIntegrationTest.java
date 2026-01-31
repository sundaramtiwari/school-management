package com.school.backend.core.student;

import com.school.backend.common.dto.PageResponse;
import com.school.backend.core.classsubject.repository.SchoolClassRepository;
import com.school.backend.core.student.dto.*;
import com.school.backend.core.student.repository.PromotionRecordRepository;
import com.school.backend.core.student.repository.StudentEnrollmentRepository;
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
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class StudentFlowIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private FeePaymentRepository feePaymentRepository;

    @Autowired
    private FeeStructureRepository feeStructureRepository;

    @Autowired
    private FeeTypeRepository feeTypeRepository;

    @Autowired
    private StudentFeeAssignmentRepository assignmentRepository;

    @Autowired
    private PromotionRecordRepository promotionRecordRepository;

    @Autowired
    private StudentEnrollmentRepository studentEnrollmentRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private SchoolClassRepository schoolClassRepository;

    @Autowired
    private SchoolRepository schoolRepository;

    private Long schoolId;
    private Long classId;
    private Long studentId;
    private Long feeStructureId;
    private Long feeTypeId;

    @Test
    void student_register_enroll_promote_flow() {

        // create school
        Map<String, Object> schoolReq = Map.of(
                "name", "Student Module School",
                "displayName", "SMS",
                "board", "CBSE",
                "schoolCode", "SMS-2025",
                "contactEmail", "sms@example.com",
                "city", "Varanasi",
                "state", "Uttar Pradesh"
        );

        ResponseEntity<School> schoolResp =
                restTemplate.postForEntity("/api/schools", schoolReq, School.class);
        Assertions.assertThat(schoolResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        schoolId = schoolResp.getBody().getId();

        // create class
        Map<String, Object> classReq =
                Map.of("name", "1", "session", "2025-26", "schoolId", schoolId);

        ResponseEntity<Map> classResp =
                restTemplate.postForEntity("/api/classes", classReq, Map.class);
        Assertions.assertThat(classResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        classId = Long.valueOf(String.valueOf(classResp.getBody().get("id")));

        // register student
        StudentCreateRequest sreq = new StudentCreateRequest();
        sreq.setAdmissionNumber("ADM-100");
        sreq.setFirstName("Test");
        sreq.setGender("Male");
        sreq.setSchoolId(schoolId);

        ResponseEntity<StudentDto> sResp =
                restTemplate.postForEntity("/api/students", sreq, StudentDto.class);
        Assertions.assertThat(sResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        studentId = sResp.getBody().getId();

        // enroll student
        StudentEnrollmentRequest er = new StudentEnrollmentRequest();
        er.setStudentId(studentId);
        er.setClassId(classId);
        er.setSession("2025-26");

        ResponseEntity<StudentEnrollmentDto> enrollResp =
                restTemplate.postForEntity("/api/enrollments", er, StudentEnrollmentDto.class);
        Assertions.assertThat(enrollResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        // --------- Setup Fee ---------

        // Create FeeType
        FeeType type = new FeeType();
        type.setName("TUITION");

        ResponseEntity<FeeType> typeResp =
                restTemplate.postForEntity("/api/fees/types", type, FeeType.class);

        feeTypeId = typeResp.getBody().getId();


        // Create FeeStructure
        FeeStructureCreateRequest fsReq = new FeeStructureCreateRequest();

        fsReq.setSchoolId(schoolId);
        fsReq.setClassId(classId);   // or fromClassId
        fsReq.setSession("2025-26");
        fsReq.setFeeTypeId(feeTypeId);
        fsReq.setAmount(10000);

        ResponseEntity<FeeStructureDto> fsResp =
                restTemplate.postForEntity("/api/fees/structures", fsReq, FeeStructureDto.class);

        feeStructureId = fsResp.getBody().getId();


        // Assign Fee
        StudentFeeAssignRequest assignReq = new StudentFeeAssignRequest();

        assignReq.setStudentId(studentId);
        assignReq.setFeeStructureId(feeStructureId);
        assignReq.setSession("2025-26");

        restTemplate.postForEntity(
                "/api/fees/assignments",
                assignReq,
                StudentFeeAssignmentDto.class
        );


        // Pay Full Fee
        FeePaymentRequest payReq = new FeePaymentRequest();

        payReq.setStudentId(studentId);
        payReq.setAmountPaid(10000);
        payReq.setMode("CASH");

        restTemplate.postForEntity(
                "/api/fees/payments",
                payReq,
                FeePaymentDto.class
        );


        // promote student
        PromotionRequest pr = new PromotionRequest();
        pr.setToClassId(classId);
        pr.setToSection("B");
        pr.setSession("2025-26");
        pr.setPromoted(true);
        pr.setFeePending(false);
        pr.setRemarks("Promoted successfully");

        ResponseEntity<PromotionRecordDto> promoteRes =
                restTemplate.postForEntity(
                        "/api/students/" + studentId + "/history/promote",
                        pr,
                        PromotionRecordDto.class
                );

        Assertions.assertThat(promoteRes.getStatusCode()).isEqualTo(HttpStatus.OK);

        // list enrollments by class
        ResponseEntity<PageResponse<StudentEnrollmentDto>> pageResp =
                restTemplate.exchange(
                        "/api/enrollments/by-class/" + classId + "?page=0&size=10",
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<PageResponse<StudentEnrollmentDto>>() {
                        }
                );

        Assertions.assertThat(pageResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(pageResp.getBody().content()).isNotEmpty();
    }

    /**
     * FK-safe cleanup after each test.
     */
    @Test
    void student_update_profile_flow() {

        // 1. Create School
        Map<String, Object> schoolReq = Map.of(
                "name", "Update Test School",
                "displayName", "UTS",
                "board", "CBSE",
                "schoolCode", "UTS-2025",
                "contactEmail", "uts@example.com",
                "city", "Varanasi",
                "state", "Uttar Pradesh"
        );

        ResponseEntity<School> schoolResp =
                restTemplate.postForEntity("/api/schools", schoolReq, School.class);

        Assertions.assertThat(schoolResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        schoolId = schoolResp.getBody().getId();


        // 2. Register Student
        StudentCreateRequest createReq = new StudentCreateRequest();

        createReq.setAdmissionNumber("ADM-200");
        createReq.setFirstName("Rahul");
        createReq.setLastName("Sharma");
        createReq.setGender("Male");
        createReq.setSchoolId(schoolId);

        ResponseEntity<StudentDto> createResp =
                restTemplate.postForEntity("/api/students", createReq, StudentDto.class);

        Assertions.assertThat(createResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        StudentDto created = createResp.getBody();

        Assertions.assertThat(created).isNotNull();

        studentId = created.getId();


        // 3. Update Student
        StudentUpdateRequest updateReq = new StudentUpdateRequest();

        updateReq.setFirstName("Rahul Updated");
        updateReq.setContactNumber("9999999999");
        updateReq.setCity("Lucknow");

        // Previous school info
        updateReq.setPreviousSchoolName("ABC Public School");
        updateReq.setPreviousSchoolBoard("ICSE");
        updateReq.setPreviousClass("5");
        updateReq.setPreviousYearOfPassing(2023);
        updateReq.setTransferCertificateNumber("TC-12345");

        ResponseEntity<StudentDto> updateResp =
                restTemplate.exchange(
                        "/api/students/" + studentId,
                        HttpMethod.PUT,
                        new org.springframework.http.HttpEntity<>(updateReq),
                        StudentDto.class
                );

        Assertions.assertThat(updateResp.getStatusCode()).isEqualTo(HttpStatus.OK);


        // 4. Fetch Updated Student
        ResponseEntity<StudentDto> getResp =
                restTemplate.getForEntity(
                        "/api/students/" + studentId,
                        StudentDto.class
                );

        Assertions.assertThat(getResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        StudentDto updated = getResp.getBody();

        Assertions.assertThat(updated).isNotNull();


        // 5. Assertions

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

    @AfterEach
    void cleanup() {

        // 1. Promotion records
        if (studentId != null) {
            promotionRecordRepository
                    .findByStudentIdOrderBySessionAsc(studentId)
                    .forEach(promotionRecordRepository::delete);
        }

        // 2. Student enrollments
        if (studentId != null) {
            studentEnrollmentRepository
                    .findByStudentId(studentId, Pageable.unpaged())
                    .forEach(studentEnrollmentRepository::delete);
        }

        // 3. Fee payments
        if (studentId != null) {
            feePaymentRepository
                    .findByStudentId(studentId)
                    .forEach(feePaymentRepository::delete);
        }

        // 4. Fee assignments
        if (studentId != null) {
            assignmentRepository
                    .findByStudentIdAndSession(studentId, "2025-26")
                    .forEach(assignmentRepository::delete);
        }

        // 5. Fee structure
        if (feeStructureId != null) {
            feeStructureRepository.deleteById(feeStructureId);
        }

        // 6. Fee type
        if (feeTypeId != null) {
            feeTypeRepository.deleteById(feeTypeId);
        }

        // 7. Student
        if (studentId != null) {
            studentRepository.deleteById(studentId);
        }

        // 8. Class
        if (classId != null) {
            schoolClassRepository.deleteById(classId);
        }

        // 9. School
        if (schoolId != null) {
            schoolRepository.deleteById(schoolId);
        }
    }
}
