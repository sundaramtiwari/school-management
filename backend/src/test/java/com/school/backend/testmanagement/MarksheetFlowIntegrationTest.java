package com.school.backend.testmanagement;

import com.school.backend.common.BaseAuthenticatedIntegrationTest;
import com.school.backend.common.TestAuthHelper;
import com.school.backend.common.enums.Gender;
import com.school.backend.core.classsubject.repository.SchoolClassRepository;
import com.school.backend.core.student.dto.StudentCreateRequest;
import com.school.backend.core.student.dto.StudentDto;
import com.school.backend.core.student.repository.StudentRepository;
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
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

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

    private Long schoolId;
    private Long classId;
    private Long studentId;
    private Long examId;
    private Long examSubjectId;

    @Test
    void full_marksheet_flow() {

        /* ---------- School ---------- */

        Map<String, Object> schoolReq = Map.of(
                "name", "Marksheet School",
                "displayName", "MS",
                "board", "CBSE",
                "schoolCode", "MS-26",
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

        schoolId = schoolResp.getBody().getId();


        /* ---------- Class ---------- */

        Map<String, Object> classReq = Map.of(
                "name", "8",
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

        classId = Long.valueOf(
                classResp.getBody().get("id").toString()
        );


        /* ---------- Student ---------- */

        StudentCreateRequest sreq =
                new StudentCreateRequest();

        sreq.setAdmissionNumber("ADM-M-1");
        sreq.setFirstName("Mark");
        sreq.setGender(Gender.MALE);
        sreq.setSchoolId(schoolId);

        HttpEntity<StudentCreateRequest> studentEntity =
                new HttpEntity<>(sreq, headers);

        ResponseEntity<StudentDto> studentResp =
                restTemplate.exchange(
                        "/api/students",
                        HttpMethod.POST,
                        studentEntity,
                        StudentDto.class
                );

        studentId = studentResp.getBody().getId();


        /* ---------- Exam ---------- */

        ExamCreateRequest examReq =
                new ExamCreateRequest();

        examReq.setSchoolId(schoolId);
        examReq.setClassId(classId);
        examReq.setSession("2025-26");
        examReq.setName("Final Exam");
        examReq.setExamType("FINAL");

        HttpEntity<ExamCreateRequest> examEntity =
                new HttpEntity<>(examReq, headers);

        ResponseEntity<Exam> examResp =
                restTemplate.exchange(
                        "/api/exams",
                        HttpMethod.POST,
                        examEntity,
                        Exam.class
                );

        examId = examResp.getBody().getId();


        /* ---------- Exam Subject ---------- */

        ExamSubjectCreateRequest esReq =
                new ExamSubjectCreateRequest();

        esReq.setExamId(examId);
        esReq.setSubjectId(1L); // fake subject id
        esReq.setMaxMarks(100);

        HttpEntity<ExamSubjectCreateRequest> esEntity =
                new HttpEntity<>(esReq, headers);

        ResponseEntity<ExamSubject> esResp =
                restTemplate.exchange(
                        "/api/exam-subjects",
                        HttpMethod.POST,
                        esEntity,
                        ExamSubject.class
                );

        examSubjectId = esResp.getBody().getId();


        /* ---------- Marks Entry ---------- */

        MarkEntryRequest markReq =
                new MarkEntryRequest();

        markReq.setExamSubjectId(examSubjectId);
        markReq.setStudentId(studentId);
        markReq.setMarksObtained(85);

        HttpEntity<MarkEntryRequest> markEntity =
                new HttpEntity<>(markReq, headers);

        restTemplate.exchange(
                "/api/marks",
                HttpMethod.POST,
                markEntity,
                StudentMark.class
        );


        /* ---------- Marksheet ---------- */

        String url =
                "/api/marksheets/exam/" + examId + "/student/" + studentId;

        HttpEntity<Void> sheetEntity =
                new HttpEntity<>(headers);

        ResponseEntity<MarksheetDto> msResp =
                restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        sheetEntity,
                        MarksheetDto.class
                );

        Assertions.assertThat(msResp.getStatusCode())
                .isEqualTo(HttpStatus.OK);

        MarksheetDto ms = msResp.getBody();

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

        if (examSubjectId != null) {

            markRepo.findAll()
                    .forEach(markRepo::delete);

            examSubjectRepo.deleteById(examSubjectId);
        }

        if (examId != null) {
            examRepo.deleteById(examId);
        }

        if (studentId != null) {
            studentRepo.deleteById(studentId);
        }

        if (classId != null) {
            classRepo.deleteById(classId);
        }

        if (schoolId != null) {
            schoolRepo.deleteById(schoolId);
        }
    }
}
