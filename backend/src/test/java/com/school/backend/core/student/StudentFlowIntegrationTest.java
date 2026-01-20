package com.school.backend.core.student;

import com.school.backend.common.dto.PageResponse;
import com.school.backend.core.classsubject.repository.SchoolClassRepository;
import com.school.backend.core.student.dto.*;
import com.school.backend.core.student.repository.PromotionRecordRepository;
import com.school.backend.core.student.repository.StudentEnrollmentRepository;
import com.school.backend.core.student.repository.StudentRepository;
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

        // 3. Student
        if (studentId != null) {
            studentRepository.deleteById(studentId);
        }

        // 4. Class
        if (classId != null) {
            schoolClassRepository.deleteById(classId);
        }

        // 5. School
        if (schoolId != null) {
            schoolRepository.deleteById(schoolId);
        }
    }
}
