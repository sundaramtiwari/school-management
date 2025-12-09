package com.school.backend.core.classsubject;

import com.school.backend.common.dto.PageResponse;
import com.school.backend.core.classsubject.dto.ClassSubjectDto;
import com.school.backend.core.classsubject.dto.SchoolClassDto;
import com.school.backend.core.classsubject.dto.SubjectDto;
import com.school.backend.school.dto.SchoolDto;
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

import java.util.List;
import java.util.Map;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class ClassSubjectFlowIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void fullFlow_createSchool_createSubject_createClass_assignSubject_and_page() {
        // 1) Create a School first (expect 201 Created)
        Map<String, Object> schoolReq = Map.of(
                "name", "Integration Test School",
                "displayName", "ITS",
                "board", "CBSE",
                "schoolCode", "ITS-2025",
                "contactEmail", "itstest@example.com",
                "city", "Varanasi",
                "state", "Uttar Pradesh"
        );

        ResponseEntity<SchoolDto> schoolResp = restTemplate.postForEntity("/api/schools", schoolReq, SchoolDto.class);
        Assertions.assertThat(schoolResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        SchoolDto createdSchool = schoolResp.getBody();
        Assertions.assertThat(createdSchool).isNotNull();
        Long schoolId = createdSchool.getId();
        Assertions.assertThat(schoolId).isNotNull();

        // 2) Create Subject (expect 201 Created)
        SubjectDto subjectReq = new SubjectDto();
        subjectReq.setName("Mathematics");
        ResponseEntity<SubjectDto> subjectResp = restTemplate.postForEntity("/api/subjects", subjectReq, SubjectDto.class);
        Assertions.assertThat(subjectResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        SubjectDto createdSubject = subjectResp.getBody();
        Assertions.assertThat(createdSubject).isNotNull();
        Long subjectId = createdSubject.getId();
        Assertions.assertThat(subjectId).isNotNull();

        // 3) Create SchoolClass (expect 201 Created)
        SchoolClassDto classReq = new SchoolClassDto();
        classReq.setName("1");
        classReq.setSession("2025-26");
        classReq.setSchoolId(schoolId);
        ResponseEntity<SchoolClassDto> classResp = restTemplate.postForEntity("/api/classes", classReq, SchoolClassDto.class);
        Assertions.assertThat(classResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        SchoolClassDto createdClass = classResp.getBody();
        Assertions.assertThat(createdClass).isNotNull();
        Long classId = createdClass.getId();
        Assertions.assertThat(classId).isNotNull();

        // 4) Assign subject to class (ClassSubject) (expect 201 Created)
        ClassSubjectDto assignReq = new ClassSubjectDto();
        assignReq.setClassId(classId);
        assignReq.setSubjectId(subjectId);
        assignReq.setSchoolId(schoolId);

        ResponseEntity<ClassSubjectDto> assignResp = restTemplate.postForEntity("/api/class-subjects", assignReq, ClassSubjectDto.class);
        Assertions.assertThat(assignResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        ClassSubjectDto createdAssignment = assignResp.getBody();
        Assertions.assertThat(createdAssignment).isNotNull();
        Assertions.assertThat(createdAssignment.getId()).isNotNull();

        // 5) Get by-class page and check content (controller returns PageResponse<T>)
        String url = "/api/class-subjects/by-class/" + classId + "?page=0&size=10";

        ParameterizedTypeReference<PageResponse<ClassSubjectDto>> ptr =
                new ParameterizedTypeReference<>() {
                };

        ResponseEntity<PageResponse<ClassSubjectDto>> pageResp = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                ptr
        );

        Assertions.assertThat(pageResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        PageResponse<ClassSubjectDto> page = pageResp.getBody();
        Assertions.assertThat(page).isNotNull();
        List<ClassSubjectDto> content = page.content();
        Assertions.assertThat(content).isNotEmpty();
        // verify the assignment exists in page content
        boolean found = content.stream().anyMatch(cs -> cs.getId().equals(createdAssignment.getId()));
        Assertions.assertThat(found).isTrue();
    }
}
