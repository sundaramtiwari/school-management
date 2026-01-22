package com.school.backend.core.student;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.backend.core.classsubject.dto.SchoolClassCreateRequest;
import com.school.backend.core.classsubject.dto.SchoolClassDto;
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
import com.school.backend.school.dto.SchoolDto;
import com.school.backend.school.repository.SchoolRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class PromotionFlowIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private ObjectMapper mapper;
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
    private Long feeTypeId;
    private Long feeStructureId;
    private Long fromClassId;
    private Long toClassId;
    private Long studentId;

    @BeforeEach
    void setup() {
        this.schoolId = createSchool("Test School");
        this.fromClassId = createClass(schoolId, "Class 1", "A", "2024-25");
        this.toClassId = createClass(schoolId, "Class 2", "B", "2025-26");
        this.studentId = createStudent(schoolId, "Amit Kumar");
        enrollStudent(studentId, fromClassId, "A", "2024-25");
    }

    // ------------------------------ TEST ----------------------------------

    @Test
    void testPromotionFlow() {
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
        fsReq.setClassId(toClassId);   // or fromClassId
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

        // Step 1: Promote student from Class 1A → Class 2B
        PromotionRequest promoteReq = new PromotionRequest();
        promoteReq.setToClassId(toClassId);
        promoteReq.setToSection("B");
        promoteReq.setSession("2025-26");
        promoteReq.setPromoted(true);
        promoteReq.setFeePending(false);
        promoteReq.setRemarks("Promoted successfully");

        ResponseEntity<PromotionRecordDto> promoteResponse =
                restTemplate.postForEntity("/api/students/" + studentId + "/history/promote",
                        promoteReq,
                        PromotionRecordDto.class);

        assertThat(promoteResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        PromotionRecordDto pr = promoteResponse.getBody();
        assertThat(pr).isNotNull();
        assertThat(pr.getStudentId()).isEqualTo(studentId);
        assertThat(pr.getToClassId()).isEqualTo(toClassId);
        assertThat(pr.getToSection()).isEqualTo("B");

        // Step 2: Check promotion history
        ResponseEntity<java.util.List<PromotionRecordDto>> promotionListResponse =
                restTemplate.exchange(
                        "/api/students/" + studentId + "/history/promotions",
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<>() {
                        });

        assertThat(promotionListResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(promotionListResponse.getBody()).hasSize(1);

        // Step 3: Check enrollment history
        ResponseEntity<java.util.List<StudentEnrollmentDto>> enrollmentListResponse =
                restTemplate.exchange(
                        "/api/students/" + studentId + "/history/enrollments",
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<>() {
                        });

        assertThat(enrollmentListResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        assertThat(enrollmentListResponse.getBody()).hasSize(2); // Class1 + Class2 enrollments

        StudentEnrollmentDto latest = enrollmentListResponse.getBody().get(1);
        assertThat(latest.getClassId()).isEqualTo(toClassId);
        assertThat(latest.getSection()).isEqualTo("B");
        assertThat(latest.getSession()).isEqualTo("2025-26");
    }

    // ------------------------------------------------------------------------

    private Long createSchool(String name) {
        SchoolCreateRequest req = new SchoolCreateRequest();
        req.setName(name);
        req.setDisplayName(name);
        req.setBoard("CBSE");
        req.setAddress("Varanasi");

        ResponseEntity<SchoolDto> res =
                restTemplate.postForEntity("/api/schools", req, SchoolDto.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return res.getBody().getId();
    }

    private Long createClass(Long schoolId, String className, String section, String session) {
        SchoolClassCreateRequest req = new SchoolClassCreateRequest();
        req.setName(className);
        req.setSection(section);
        req.setSession(session);
        req.setSchoolId(schoolId);

        ResponseEntity<SchoolClassDto> res =
                restTemplate.postForEntity("/api/classes", req, SchoolClassDto.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        return res.getBody().getId();
    }

    private Long createStudent(Long schoolId, String name) {
        StudentCreateRequest req = new StudentCreateRequest();
        req.setFirstName(name);
        req.setSchoolId(schoolId);
        req.setDob(LocalDate.of(2015, 1, 1));
        req.setGender("MALE");
        req.setAdmissionNumber("name" + System.currentTimeMillis());

        ResponseEntity<StudentDto> res =
                restTemplate.postForEntity("/api/students", req, StudentDto.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        return res.getBody().getId();
    }

    private void enrollStudent(Long studentId, Long classId, String section, String session) {
        StudentEnrollmentRequest req = new StudentEnrollmentRequest();
        req.setStudentId(studentId);
        req.setClassId(classId);
        req.setSection(section);
        req.setSession(session);

        ResponseEntity<StudentEnrollmentDto> res =
                restTemplate.postForEntity("/api/enrollments", req, StudentEnrollmentDto.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    /**
     * FK-safe cleanup after each test.
     */
    @AfterEach
    void cleanup() {

        // 1. Promotion records
        if (studentId != null) {
            promotionRecordRepository
                    .findByStudentIdOrderBySessionAsc(studentId)
                    .forEach(promotionRecordRepository::delete);
        }

        // 2. Enrollments
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
        if (toClassId != null) {
            schoolClassRepository.deleteById(toClassId);
        }

        if (fromClassId != null) {
            schoolClassRepository.deleteById(fromClassId);
        }

        // 9. School
        if (schoolId != null) {
            schoolRepository.deleteById(schoolId);
        }
    }

}
