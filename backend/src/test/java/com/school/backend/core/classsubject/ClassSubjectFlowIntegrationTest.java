package com.school.backend.core.classsubject;

import com.school.backend.common.dto.PageResponse;
import com.school.backend.core.classsubject.dto.ClassSubjectDto;
import com.school.backend.core.classsubject.dto.SchoolClassDto;
import com.school.backend.core.classsubject.dto.SubjectDto;
import com.school.backend.core.classsubject.repository.ClassSubjectRepository;
import com.school.backend.core.classsubject.repository.SchoolClassRepository;
import com.school.backend.core.classsubject.repository.SubjectRepository;
import com.school.backend.school.dto.SchoolDto;
import com.school.backend.school.repository.SchoolRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
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
public class ClassSubjectFlowIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ClassSubjectRepository classSubjectRepository;

    @Autowired
    private SchoolClassRepository schoolClassRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private SchoolRepository schoolRepository;

    private Long schoolId;
    private Long classId;
    private Long subjectId;
    private Long classSubjectId;

    @Test
    void fullFlow_createSchool_createSubject_createClass_assignSubject_and_page() {
        // 1) Create School
        Map<String, Object> schoolReq = Map.of(
                "name", "Integration Test School",
                "displayName", "ITS",
                "board", "CBSE",
                "schoolCode", "ITS-2025",
                "contactEmail", "itstest@example.com",
                "city", "Varanasi",
                "state", "Uttar Pradesh"
        );

        ResponseEntity<SchoolDto> schoolResp =
                restTemplate.postForEntity("/api/schools", schoolReq, SchoolDto.class);

        Assertions.assertThat(schoolResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        schoolId = schoolResp.getBody().getId();

        // 2) Create Subject
        SubjectDto subjectReq = new SubjectDto();
        subjectReq.setName("Mathematics");

        ResponseEntity<SubjectDto> subjectResp =
                restTemplate.postForEntity("/api/subjects", subjectReq, SubjectDto.class);

        Assertions.assertThat(subjectResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        subjectId = subjectResp.getBody().getId();

        // 3) Create Class
        SchoolClassDto classReq = new SchoolClassDto();
        classReq.setName("1");
        classReq.setSession("2025-26");
        classReq.setSchoolId(schoolId);

        ResponseEntity<SchoolClassDto> classResp =
                restTemplate.postForEntity("/api/classes", classReq, SchoolClassDto.class);

        Assertions.assertThat(classResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        classId = classResp.getBody().getId();

        // 4) Assign Subject to Class
        ClassSubjectDto assignReq = new ClassSubjectDto();
        assignReq.setClassId(classId);
        assignReq.setSubjectId(subjectId);
        assignReq.setSchoolId(schoolId);

        ResponseEntity<ClassSubjectDto> assignResp =
                restTemplate.postForEntity("/api/class-subjects", assignReq, ClassSubjectDto.class);

        Assertions.assertThat(assignResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        classSubjectId = assignResp.getBody().getId();

        // 5) Verify paging
        String url = "/api/class-subjects/by-class/" + classId + "?page=0&size=10";

        ParameterizedTypeReference<PageResponse<ClassSubjectDto>> ptr =
                new ParameterizedTypeReference<>() {
                };

        ResponseEntity<PageResponse<ClassSubjectDto>> pageResp =
                restTemplate.exchange(url, HttpMethod.GET, null, ptr);

        Assertions.assertThat(pageResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(pageResp.getBody().content())
                .extracting(ClassSubjectDto::getId)
                .contains(classSubjectId);
    }

    /**
     * Cleanup runs after EACH test method.
     * Order is critical to avoid FK violations.
     */
    @AfterEach
    void cleanup() {
        if (classSubjectId != null) {
            classSubjectRepository.deleteById(classSubjectId);
        }

        if (classId != null) {
            schoolClassRepository.deleteById(classId);
        }

        if (subjectId != null) {
            subjectRepository.deleteById(subjectId);
        }

        if (schoolId != null) {
            schoolRepository.deleteById(schoolId);
        }
    }
}
