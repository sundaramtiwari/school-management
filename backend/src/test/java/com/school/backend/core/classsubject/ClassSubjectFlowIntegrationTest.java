package com.school.backend.core.classsubject;

import com.school.backend.common.BaseAuthenticatedIntegrationTest;
import com.school.backend.common.dto.PageResponse;
import com.school.backend.core.classsubject.dto.ClassSubjectDto;
import com.school.backend.core.classsubject.dto.SchoolClassDto;
import com.school.backend.core.classsubject.dto.SubjectDto;
import com.school.backend.core.classsubject.repository.ClassSubjectRepository;
import com.school.backend.core.classsubject.repository.SchoolClassRepository;
import com.school.backend.core.classsubject.repository.SubjectRepository;
import com.school.backend.school.dto.SchoolDto;
import com.school.backend.school.entity.AcademicSession;
import com.school.backend.school.repository.SchoolRepository;
import com.school.backend.user.repository.UserRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.Objects;

public class ClassSubjectFlowIntegrationTest extends BaseAuthenticatedIntegrationTest {

    @Autowired
    private ClassSubjectRepository classSubjectRepository;
    @Autowired
    private SchoolClassRepository schoolClassRepository;
    @Autowired
    private SubjectRepository subjectRepository;
    @Autowired
    private SchoolRepository schoolRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private com.school.backend.school.repository.AcademicSessionRepository sessionRepo;

    private Long schoolId;
    private Long classId;
    private Long subjectId;
    private Long classSubjectId;
    private Long sessionId;

    @Test
    void fullFlow_createSchool_createSubject_createClass_assignSubject_and_page() {

        var headers = authHelper.authHeaders(token);

        // 1) Create School
        Map<String, Object> schoolReq = Map.of(
                "name", "Integration Test School",
                "displayName", "ITS",
                "board", "CBSE",
                "schoolCode", "ITS-2025",
                "contactEmail", "itstest@example.com",
                "city", "Varanasi",
                "state", "Uttar Pradesh");

        var schoolEntity = new HttpEntity<>(schoolReq, headers);

        ResponseEntity<SchoolDto> schoolResp = restTemplate.exchange(
                "/api/schools",
                HttpMethod.POST,
                schoolEntity,
                SchoolDto.class);

        Assertions.assertThat(schoolResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        schoolId = Objects.requireNonNull(schoolResp.getBody()).getId();

        // LOGIN AS SCHOOL ADMIN NOW
        loginAsSchoolAdmin(schoolId);
        // LOGIN AS SCHOOL ADMIN NOW
        loginAsSchoolAdmin(schoolId);
        headers = authHelper.authHeaders(token);

        // 1.5) Create Session
        AcademicSession session = AcademicSession
                .builder()
                .name("2025-26")
                .schoolId(schoolId)
                .active(true)
                .build();
        session = sessionRepo.save(session);
        sessionId = session.getId();

        // 2) Create Subject
        SubjectDto subjectReq = new SubjectDto();
        subjectReq.setName("Mathematics");

        var subjectEntity = new HttpEntity<>(subjectReq, headers);

        ResponseEntity<SubjectDto> subjectResp = restTemplate.exchange(
                "/api/subjects",
                HttpMethod.POST,
                subjectEntity,
                SubjectDto.class);

        Assertions.assertThat(subjectResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        subjectId = Objects.requireNonNull(subjectResp.getBody()).getId();

        // 3) Create Class
        SchoolClassDto classReq = new SchoolClassDto();
        classReq.setName("1");
        classReq.setSessionId(sessionId);

        var classEntity = new HttpEntity<>(classReq, headers);

        ResponseEntity<SchoolClassDto> classResp = restTemplate.exchange(
                "/api/classes",
                HttpMethod.POST,
                classEntity,
                SchoolClassDto.class);

        Assertions.assertThat(classResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        classId = Objects.requireNonNull(classResp.getBody()).getId();

        // 4) Assign Subject to Class
        ClassSubjectDto assignReq = new ClassSubjectDto();
        assignReq.setClassId(classId);
        assignReq.setSubjectId(subjectId);

        var assignEntity = new HttpEntity<>(assignReq, headers);

        ResponseEntity<ClassSubjectDto> assignResp = restTemplate.exchange(
                "/api/class-subjects",
                HttpMethod.POST,
                assignEntity,
                ClassSubjectDto.class);

        Assertions.assertThat(assignResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        classSubjectId = Objects.requireNonNull(assignResp.getBody()).getId();

        // 5) Verify paging
        String url = "/api/class-subjects/by-class/" + classId + "?page=0&size=10";

        var pageEntity = new HttpEntity<>(headers);

        ParameterizedTypeReference<PageResponse<ClassSubjectDto>> ptr = new ParameterizedTypeReference<>() {
        };

        ResponseEntity<PageResponse<ClassSubjectDto>> pageResp = restTemplate.exchange(
                url,
                HttpMethod.GET,
                pageEntity,
                ptr);

        Assertions.assertThat(pageResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        Assertions.assertThat(Objects.requireNonNull(pageResp.getBody()).content())
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

        // 6. Users (IMPORTANT)
        if (schoolId != null) {
            userRepository
                    .findAll()
                    .stream()
                    .filter(u -> u.getSchool() != null &&
                            u.getSchool().getId().equals(schoolId))
                    .forEach(userRepository::delete);
        }

        if (sessionId != null) {
            sessionRepo.deleteById(sessionId);
        }

        if (schoolId != null) {
            schoolRepository.deleteById(schoolId);
        }
    }
}
