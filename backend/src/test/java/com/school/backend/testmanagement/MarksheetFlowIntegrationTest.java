package com.school.backend.testmanagement;

import com.school.backend.common.BaseAuthenticatedIntegrationTest;
import com.school.backend.common.enums.Gender;
import com.school.backend.core.classsubject.entity.Subject;
import com.school.backend.core.classsubject.repository.SchoolClassRepository;
import com.school.backend.core.classsubject.repository.SubjectRepository;
import com.school.backend.core.guardian.dto.GuardianCreateRequest;
import com.school.backend.core.student.dto.StudentCreateRequest;
import com.school.backend.core.student.dto.StudentDto;
import com.school.backend.core.student.dto.StudentEnrollmentDto;
import com.school.backend.core.student.dto.StudentEnrollmentRequest;
import com.school.backend.core.student.repository.StudentRepository;
import com.school.backend.school.entity.AcademicSession;
import com.school.backend.school.entity.School;
import com.school.backend.school.repository.SchoolRepository;
import com.school.backend.testmanagement.dto.ExamCreateRequest;
import com.school.backend.testmanagement.dto.ExamSubjectCreateRequest;
import com.school.backend.testmanagement.dto.MarkEntryRequest;
import com.school.backend.testmanagement.dto.MarksheetDto;
import com.school.backend.testmanagement.entity.Exam;
import com.school.backend.testmanagement.entity.ExamSubject;
import com.school.backend.testmanagement.entity.StudentMark;
import com.school.backend.testmanagement.repository.ExamRepository;
import com.school.backend.testmanagement.repository.ExamSubjectRepository;
import com.school.backend.testmanagement.repository.StudentMarkRepository;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MarksheetFlowIntegrationTest extends BaseAuthenticatedIntegrationTest {
    @Autowired
    private SchoolRepository schoolRepo;
    @Autowired
    private SchoolClassRepository classRepo;
    @Autowired
    private StudentRepository studentRepo;
    @Autowired
    private ExamRepository examRepo;
    @Autowired
    private ExamSubjectRepository examSubjectRepo;
    @Autowired
    private StudentMarkRepository markRepo;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private SubjectRepository subjectRepo;
    @Autowired
    private com.school.backend.school.repository.AcademicSessionRepository sessionRepo;

    private Long schoolId;
    private Long classId;
    private Long studentId;
    private Long examId;
    private Long subjectId;
    private Long examSubjectId;
    private Long sessionId;

    @Test
    void full_marksheet_flow() {

        /* ---------- School ---------- */

        Map<String, Object> schoolReq = Map.of(
                "name", "Marksheet School",
                "displayName", "MS",
                "board", "CBSE",
                "schoolCode", "MS-26",
                "city", "Varanasi",
                "state", "UP");

        HttpEntity<Map<String, Object>> schoolEntity = new HttpEntity<>(schoolReq, headers);

        ResponseEntity<School> schoolResp = restTemplate.exchange(
                "/api/schools",
                HttpMethod.POST,
                schoolEntity,
                School.class);

        schoolId = Objects.requireNonNull(schoolResp.getBody()).getId();

        // LOGIN AS SCHOOL ADMIN NOW
        loginAsSchoolAdmin(schoolId);

        /* ---------- Session ---------- */
        AcademicSession session = AcademicSession
                .builder()
                .name("2025-26")
                .startDate(LocalDate.of(2025, 4, 1))
                .endDate(LocalDate.of(2026, 3, 31))
                .schoolId(schoolId)
                .active(true)
                .build();
        session = sessionRepo.save(session);
        sessionId = session.getId();

        School school = schoolRepo.findById(schoolId).orElseThrow();
        school.setCurrentSessionId(sessionId);
        schoolRepo.save(school);

        setSessionHeader(sessionId);

        /* ---------- Class ---------- */

        Map<String, Object> classReq = Map.of(
                "name", "8",
                "sessionId", sessionId,
                "schoolId", schoolId);

        HttpEntity<Map<String, Object>> classEntity = new HttpEntity<>(classReq, headers);

        ResponseEntity<Map<String, Object>> classResp = restTemplate.exchange(
                "/api/classes",
                HttpMethod.POST,
                classEntity,
                new ParameterizedTypeReference<>() {
                });

        classId = Long.valueOf(
                Objects.requireNonNull(classResp.getBody()).get("id").toString());

        /* ---------- Student ---------- */

        StudentCreateRequest sreq = new StudentCreateRequest();

        sreq.setAdmissionNumber("ADM-M-1");
        sreq.setFirstName("Mark");
        sreq.setGender(Gender.MALE);
        sreq.setGuardians(List.of(GuardianCreateRequest.builder()
                .name("Marksheet Guardian")
                .contactNumber("4433221100")
                .relation("FATHER")
                .primaryGuardian(true)
                .build()));

        HttpEntity<StudentCreateRequest> studentEntity = new HttpEntity<>(sreq, headers);

        ResponseEntity<StudentDto> studentResp = restTemplate.exchange(
                "/api/students",
                HttpMethod.POST,
                studentEntity,
                StudentDto.class);

        studentId = Objects.requireNonNull(studentResp.getBody()).getId();

        /* ---------- Enrollment ---------- */

        StudentEnrollmentRequest enrollReq = new StudentEnrollmentRequest();
        enrollReq.setStudentId(studentId);
        enrollReq.setClassId(classId);
        enrollReq.setSessionId(sessionId);

        HttpEntity<StudentEnrollmentRequest> enrollEntity = new HttpEntity<>(enrollReq, headers);
        ResponseEntity<StudentEnrollmentDto> enrollResp = restTemplate.exchange(
                "/api/enrollments",
                HttpMethod.POST,
                enrollEntity,
                StudentEnrollmentDto.class);

        Assertions.assertThat(enrollResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        /* ---------- Exam ---------- */

        ExamCreateRequest examReq = new ExamCreateRequest();
        examReq.setClassId(classId);
        examReq.setSessionId(sessionId);
        examReq.setName("Final Exam");
        examReq.setExamType("FINAL");

        HttpEntity<ExamCreateRequest> examEntity = new HttpEntity<>(examReq, headers);

        ResponseEntity<Exam> examResp = restTemplate.exchange(
                "/api/exams",
                HttpMethod.POST,
                examEntity,
                Exam.class);

        examId = Objects.requireNonNull(examResp.getBody()).getId();

        /* ---------- Subject ---------- */

        Subject sub = Subject.builder()
                .name("Mathematics")
                .code("MATH")
                .build();
        sub.setSchoolId(schoolId);

        sub = subjectRepo.save(sub);
        subjectId = sub.getId();

        /* ---------- Exam Subject ---------- */

        ExamSubjectCreateRequest esReq = new ExamSubjectCreateRequest();

        esReq.setExamId(examId);
        esReq.setSubjectId(subjectId);
        esReq.setMaxMarks(100);

        HttpEntity<ExamSubjectCreateRequest> esEntity = new HttpEntity<>(esReq, headers);

        ResponseEntity<ExamSubject> esResp = restTemplate.exchange(
                "/api/exam-subjects",
                HttpMethod.POST,
                esEntity,
                ExamSubject.class);

        examSubjectId = Objects.requireNonNull(esResp.getBody()).getId();

        /* ---------- Marks Entry ---------- */

        MarkEntryRequest markReq = new MarkEntryRequest();

        markReq.setExamSubjectId(examSubjectId);
        markReq.setStudentId(studentId);
        markReq.setMarksObtained(85);

        HttpEntity<MarkEntryRequest> markEntity = new HttpEntity<>(markReq, headers);

        restTemplate.exchange(
                "/api/marks",
                HttpMethod.POST,
                markEntity,
                StudentMark.class);

        /* ---------- Publish Exam ---------- */

        ResponseEntity<Map<String, Object>> publishResp = restTemplate.exchange(
                "/api/exams/" + examId + "/publish",
                HttpMethod.PUT,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {
                });

        Assertions.assertThat(publishResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        /* ---------- Marksheet ---------- */

        String url = "/api/marksheets/exam/" + examId + "/student/" + studentId;

        HttpEntity<Void> sheetEntity = new HttpEntity<>(headers);

        ResponseEntity<MarksheetDto> msResp = restTemplate.exchange(
                url,
                HttpMethod.GET,
                sheetEntity,
                MarksheetDto.class);

        Assertions.assertThat(msResp.getStatusCode())
                .isEqualTo(HttpStatus.OK);

        MarksheetDto ms = Objects.requireNonNull(msResp.getBody());

        Assertions.assertThat(ms).isNotNull();

        Assertions.assertThat(ms.getTotalMarks()).isEqualTo(85);
        Assertions.assertThat(ms.getMaxMarks()).isEqualTo(100);

        Assertions.assertThat(ms.getPercentage()).isEqualTo(85.0);

        Assertions.assertThat(ms.isPassed()).isTrue();

        Assertions.assertThat(ms.getGrade()).isEqualTo("A");

        Assertions.assertThat(ms.getSubjects()).hasSize(1);
    }

    // ------------------------------------------------

    @AfterEach
    void cleanup() {
        fullCleanup();
    }

}
