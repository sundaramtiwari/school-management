package com.school.backend.fee;

import com.school.backend.common.BaseAuthenticatedIntegrationTest;
import com.school.backend.common.enums.FeeFrequency;
import com.school.backend.common.enums.Gender;
import com.school.backend.common.enums.UserRole;
import com.school.backend.core.classsubject.entity.SchoolClass;
import com.school.backend.core.student.dto.StudentEnrollmentRequest;
import com.school.backend.core.student.entity.Student;
import com.school.backend.core.student.entity.StudentEnrollment;
import com.school.backend.fee.dto.FeeStructureCreateRequest;
import com.school.backend.fee.dto.FeeStructureDto;
import com.school.backend.fee.entity.FeeType;
import com.school.backend.school.entity.School;
import com.school.backend.user.dto.AuthResponse;
import com.school.backend.user.dto.LoginRequest;
import com.school.backend.user.dto.UserDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FeeStructureManagementIntegrationTest extends BaseAuthenticatedIntegrationTest {

    private School school;
    private Long schoolId;
    private Long sessionId;
    private Long classId;
    private Long feeTypeId;

    @BeforeEach
    void setup() {
        fullCleanup();

        school = schoolRepository.findBySchoolCode("FSM-001")
                .orElseGet(() -> {
                    School s = new School();
                    s.setName("Fee Structure Mgmt School");
                    s.setSchoolCode("FSM-001");
                    s.setActive(true);
                    return schoolRepository.save(s);
                });
        schoolId = school.getId();

        loginAsSchoolAdmin(schoolId);
        sessionId = setupSession(schoolId, sessionRepository, schoolRepository);
        setSessionHeader(sessionId);

        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setName("8");
        schoolClass.setSection("A");
        schoolClass.setSessionId(sessionId);
        schoolClass.setSchoolId(schoolId);
        schoolClass.setActive(true);
        classId = schoolClassRepository.save(schoolClass).getId();

        FeeType feeType = new FeeType();
        feeType.setName("Tuition");
        feeType.setDescription("Tuition");
        feeType.setSchoolId(schoolId);
        feeType.setActive(true);
        feeTypeId = feeTypeRepository.save(feeType).getId();
    }

    @Test
    void editingStructure_shouldNotModifyExistingAssignments() {
        Long studentId = createStudentAndEnrollment("ADM-FSM-01", true);

        Long structureId = createStructure(feeTypeId, BigDecimal.valueOf(1000), FeeFrequency.ONE_TIME);
        var assignments = assignmentRepository.findByStudentIdAndSessionId(studentId, sessionId);
        assertEquals(1, assignments.size());
        BigDecimal oldAssignedAmount = assignments.get(0).getAmount();

        Map<String, Object> patchReq = Map.of(
                "amount", 2000,
                "frequency", "ANNUALLY",
                "dueDayOfMonth", 15);
        ResponseEntity<FeeStructureDto> patchResp = restTemplate.exchange(
                "/api/fees/structures/" + structureId,
                HttpMethod.PATCH,
                new HttpEntity<>(patchReq, headers),
                FeeStructureDto.class);
        assertEquals(HttpStatus.OK, patchResp.getStatusCode());

        var postUpdateAssignments = assignmentRepository.findByStudentIdAndSessionId(studentId, sessionId);
        assertEquals(1, postUpdateAssignments.size());
        assertEquals(0, oldAssignedAmount.compareTo(postUpdateAssignments.get(0).getAmount()));
    }

    @Test
    void deactivatedStructure_shouldNotGenerateAssignmentsOnNewEnrollment() {
        Long structureId = createStructure(feeTypeId, BigDecimal.valueOf(1800), FeeFrequency.ONE_TIME);
        ResponseEntity<FeeStructureDto> toggleResp = restTemplate.exchange(
                "/api/fees/structures/" + structureId + "/toggle",
                HttpMethod.PATCH,
                new HttpEntity<>(headers),
                FeeStructureDto.class);
        assertEquals(HttpStatus.OK, toggleResp.getStatusCode());
        assertFalse(Objects.requireNonNull(toggleResp.getBody()).isActive());

        Long studentId = createStudent("ADM-FSM-02");
        StudentEnrollmentRequest enrollmentRequest = new StudentEnrollmentRequest();
        enrollmentRequest.setStudentId(studentId);
        enrollmentRequest.setClassId(classId);
        enrollmentRequest.setSessionId(sessionId);
        enrollmentRequest.setEnrollmentDate(LocalDate.now());
        ResponseEntity<Map> enrollResp = restTemplate.exchange(
                "/api/enrollments",
                HttpMethod.POST,
                new HttpEntity<>(enrollmentRequest, headers),
                Map.class);
        assertEquals(HttpStatus.OK, enrollResp.getStatusCode());

        List<?> assignments = assignmentRepository.findByStudentIdAndSessionId(studentId, sessionId);
        assertTrue(assignments.isEmpty());
    }

    @Test
    void activeStructure_shouldGenerateAssignmentsOnNewEnrollment() {
        createStructure(feeTypeId, BigDecimal.valueOf(2200), FeeFrequency.ONE_TIME);

        Long studentId = createStudent("ADM-FSM-03");
        StudentEnrollmentRequest enrollmentRequest = new StudentEnrollmentRequest();
        enrollmentRequest.setStudentId(studentId);
        enrollmentRequest.setClassId(classId);
        enrollmentRequest.setSessionId(sessionId);
        enrollmentRequest.setEnrollmentDate(LocalDate.now());
        ResponseEntity<Map> enrollResp = restTemplate.exchange(
                "/api/enrollments",
                HttpMethod.POST,
                new HttpEntity<>(enrollmentRequest, headers),
                Map.class);
        assertEquals(HttpStatus.OK, enrollResp.getStatusCode());

        List<?> assignments = assignmentRepository.findByStudentIdAndSessionId(studentId, sessionId);
        assertEquals(1, assignments.size());
    }

    @Test
    void ownershipEnforcement_shouldBlockCrossTenantStructureUpdate() {
        Long otherSchoolStructureId = createStructureInOtherSchool();

        Map<String, Object> patchReq = Map.of("amount", 3000);
        ResponseEntity<Map> patchResp = restTemplate.exchange(
                "/api/fees/structures/" + otherSchoolStructureId,
                HttpMethod.PATCH,
                new HttpEntity<>(patchReq, headers),
                Map.class);
        assertEquals(HttpStatus.FORBIDDEN, patchResp.getStatusCode());
    }

    @Test
    void roleEnforcement_shouldBlockAccountantFromStructurePatch() {
        Long structureId = createStructure(feeTypeId, BigDecimal.valueOf(1500), FeeFrequency.ONE_TIME);

        String accountantEmail = "acct" + schoolId + "@test.com";
        String accountantPassword = "admin123";

        UserDto createReq = new UserDto();
        createReq.setEmail(accountantEmail);
        createReq.setPassword(accountantPassword);
        createReq.setFullName("Fee Accountant");
        createReq.setRole(UserRole.ACCOUNTANT);
        ResponseEntity<UserDto> createResp = restTemplate.exchange(
                "/api/users",
                HttpMethod.POST,
                new HttpEntity<>(createReq, headers),
                UserDto.class);
        assertEquals(HttpStatus.CREATED, createResp.getStatusCode());

        String accountantToken = login(accountantEmail, accountantPassword);
        var accountantHeaders = authHelper.authHeaders(accountantToken);
        accountantHeaders.set("X-Session-Id", String.valueOf(sessionId));

        Map<String, Object> patchReq = Map.of("amount", 1900);
        ResponseEntity<Map> patchResp = restTemplate.exchange(
                "/api/fees/structures/" + structureId,
                HttpMethod.PATCH,
                new HttpEntity<>(patchReq, accountantHeaders),
                Map.class);
        assertEquals(HttpStatus.FORBIDDEN, patchResp.getStatusCode());
    }

    @Test
    void deactivatedFeeType_shouldBlockNewStructureCreation() {
        ResponseEntity<FeeType> toggleResp = restTemplate.exchange(
                "/api/fees/types/" + feeTypeId + "/toggle",
                HttpMethod.PATCH,
                new HttpEntity<>(headers),
                FeeType.class);
        assertEquals(HttpStatus.OK, toggleResp.getStatusCode());
        assertFalse(Objects.requireNonNull(toggleResp.getBody()).isActive());

        FeeStructureCreateRequest createReq = new FeeStructureCreateRequest();
        createReq.setClassId(classId);
        createReq.setSessionId(sessionId);
        createReq.setFeeTypeId(feeTypeId);
        createReq.setAmount(BigDecimal.valueOf(2500));
        createReq.setFrequency(FeeFrequency.ONE_TIME);
        ResponseEntity<Map> createResp = restTemplate.exchange(
                "/api/fees/structures",
                HttpMethod.POST,
                new HttpEntity<>(createReq, headers),
                Map.class);
        assertEquals(HttpStatus.BAD_REQUEST, createResp.getStatusCode());
    }

    private Long createStructure(Long feeTypeId, BigDecimal amount, FeeFrequency frequency) {
        FeeStructureCreateRequest createReq = new FeeStructureCreateRequest();
        createReq.setClassId(classId);
        createReq.setSessionId(sessionId);
        createReq.setFeeTypeId(feeTypeId);
        createReq.setAmount(amount);
        createReq.setFrequency(frequency);

        ResponseEntity<FeeStructureDto> resp = restTemplate.exchange(
                "/api/fees/structures",
                HttpMethod.POST,
                new HttpEntity<>(createReq, headers),
                FeeStructureDto.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        return Objects.requireNonNull(resp.getBody()).getId();
    }

    private Long createStudentAndEnrollment(String admissionNumber, boolean enrolled) {
        Long studentId = createStudent(admissionNumber);
        if (enrolled) {
            StudentEnrollment enrollment = new StudentEnrollment();
            enrollment.setStudentId(studentId);
            enrollment.setClassId(classId);
            enrollment.setSessionId(sessionId);
            enrollment.setSchoolId(schoolId);
            enrollment.setActive(true);
            studentEnrollmentRepository.save(enrollment);
        }
        return studentId;
    }

    private Long createStudent(String admissionNumber) {
        Student student = new Student();
        student.setFirstName("Fee");
        student.setLastName("Student");
        student.setAdmissionNumber(admissionNumber);
        student.setGender(Gender.MALE);
        student.setSchoolId(schoolId);
        student.setCurrentClass(schoolClassRepository.findById(classId).orElse(null));
        student.setActive(true);
        return studentRepository.save(student).getId();
    }

    private Long createStructureInOtherSchool() {
        School otherSchool = new School();
        otherSchool.setName("Other School");
        otherSchool.setSchoolCode("FSM-002");
        otherSchool.setActive(true);
        otherSchool = schoolRepository.save(otherSchool);

        loginAsSchoolAdmin(otherSchool.getId());
        Long otherSessionId = setupSession(otherSchool.getId(), sessionRepository, schoolRepository);
        setSessionHeader(otherSessionId);

        SchoolClass otherClass = new SchoolClass();
        otherClass.setName("9");
        otherClass.setSection("B");
        otherClass.setSessionId(otherSessionId);
        otherClass.setSchoolId(otherSchool.getId());
        otherClass.setActive(true);
        otherClass = schoolClassRepository.save(otherClass);

        FeeType otherType = new FeeType();
        otherType.setName("Lab Fee");
        otherType.setDescription("Lab Fee");
        otherType.setSchoolId(otherSchool.getId());
        otherType.setActive(true);
        otherType = feeTypeRepository.save(otherType);

        FeeStructureCreateRequest req = new FeeStructureCreateRequest();
        req.setClassId(otherClass.getId());
        req.setSessionId(otherSessionId);
        req.setFeeTypeId(otherType.getId());
        req.setAmount(BigDecimal.valueOf(500));
        req.setFrequency(FeeFrequency.ONE_TIME);
        ResponseEntity<FeeStructureDto> resp = restTemplate.exchange(
                "/api/fees/structures",
                HttpMethod.POST,
                new HttpEntity<>(req, headers),
                FeeStructureDto.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        Long structureId = Objects.requireNonNull(resp.getBody()).getId();

        // Return to original tenant context
        loginAsSchoolAdmin(schoolId);
        setSessionHeader(sessionId);
        return structureId;
    }

    private String login(String email, String password) {
        LoginRequest req = new LoginRequest();
        req.setEmail(email);
        req.setPassword(password);
        ResponseEntity<AuthResponse> resp = restTemplate.postForEntity("/api/auth/login", req, AuthResponse.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        return Objects.requireNonNull(resp.getBody()).getToken();
    }

    @AfterEach
    void teardown() {
        fullCleanup();
    }
}
