package com.school.backend.core.student;

import com.school.backend.common.dto.PageResponse;
import com.school.backend.core.student.dto.*;
import com.school.backend.school.entity.School;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
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

        ResponseEntity<School> schoolResp = restTemplate.postForEntity("/api/schools", schoolReq, School.class);
        Assertions.assertThat(schoolResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long schoolId = schoolResp.getBody().getId();

        // create class
        Map<String, Object> classReq = Map.of("name", "1", "session", "2025-26", "schoolId", schoolId);
        ResponseEntity<Map> classResp = restTemplate.postForEntity("/api/classes", classReq, Map.class);
        Assertions.assertThat(classResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        Long classId = Long.valueOf(String.valueOf(((Map) classResp.getBody()).get("id")));

        // register student
        StudentCreateRequest sreq = new StudentCreateRequest();
        sreq.setAdmissionNumber("ADM-100");
        sreq.setFirstName("Test");
        sreq.setGender("Male");
        sreq.setSchoolId(schoolId);
        ResponseEntity<StudentDto> sResp = restTemplate.postForEntity("/api/students", sreq, StudentDto.class);
        Assertions.assertThat(sResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        StudentDto created = sResp.getBody();
        Assertions.assertThat(created).isNotNull();

        // enroll student
        StudentEnrollmentRequest er = new StudentEnrollmentRequest();
        er.setStudentId(created.getId());
        er.setClassId(classId);
        er.setSession("2025-26");
        ResponseEntity<StudentEnrollmentDto> enrollResp = restTemplate.postForEntity("/api/enrollments", er, StudentEnrollmentDto.class);
        Assertions.assertThat(enrollResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        // promote student
        PromotionRequest pr = new PromotionRequest();
        pr.setStudentId(created.getId());
        pr.setFromClassId(classId);
        pr.setToClassId(classId); // promote to same for test simplicity
        pr.setSession("2025-26");
        ResponseEntity<Void> promoResp = restTemplate.postForEntity("/api/enrollments/promote", pr, Void.class);
        Assertions.assertThat(promoResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        // list enrollments by class and assert PageResponse shape
        ResponseEntity<PageResponse<StudentEnrollmentDto>> pageResp =
                restTemplate.exchange("/api/enrollments/by-class/" + classId + "?page=0&size=10",
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<PageResponse<StudentEnrollmentDto>>() {
                        });

        Assertions.assertThat(pageResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        PageResponse<StudentEnrollmentDto> prBody = pageResp.getBody();
        Assertions.assertThat(prBody).isNotNull();
        Assertions.assertThat(prBody.content()).isNotEmpty();
    }
}
