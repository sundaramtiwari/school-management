package com.school.backend.core.guardian;

import com.school.backend.common.enums.Gender;
import com.school.backend.common.tenant.SessionContext;
import com.school.backend.common.tenant.TenantContext;
import com.school.backend.core.classsubject.entity.SchoolClass;
import com.school.backend.core.classsubject.repository.SchoolClassRepository;
import com.school.backend.core.guardian.dto.GuardianCreateRequest;
import com.school.backend.core.guardian.entity.Guardian;
import com.school.backend.core.guardian.repository.GuardianRepository;
import com.school.backend.core.student.dto.StudentCreateRequest;
import com.school.backend.core.student.entity.StudentGuardian;
import com.school.backend.core.student.repository.StudentGuardianRepository;
import com.school.backend.core.student.service.StudentService;
import com.school.backend.school.entity.AcademicSession;
import com.school.backend.school.entity.School;
import com.school.backend.school.repository.AcademicSessionRepository;
import com.school.backend.school.repository.SchoolRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
public class GuardianFlowIntegrationTest {
    private static final String TEST_SCHOOL_CODE = "TEST-GUARDIAN-SCHOOL";

    @Autowired
    private StudentService studentService;

    @Autowired
    private GuardianRepository guardianRepository;

    @Autowired
    private StudentGuardianRepository studentGuardianRepository;

    @Autowired
    private SchoolRepository schoolRepository;

    @Autowired
    private AcademicSessionRepository sessionRepository;

    @Autowired
    private SchoolClassRepository schoolClassRepository;

    @BeforeEach
    void setUp() {
        SessionContext.clear();
        Long schoolId = ensureSchoolId();
        TenantContext.setSchoolId(schoolId);
        Long sessionId = ensureSessionForSchool(schoolId);
        ensureClassExists(schoolId, sessionId);
        SessionContext.setSessionId(sessionId);
    }

    @AfterEach
    void tearDown() {
        SessionContext.clear();
        TenantContext.clear();
    }

    @Test
    void testGuardianReuseForSiblings() {
        // 1. Create first student with a guardian
        GuardianCreateRequest fatherReq = GuardianCreateRequest.builder()
                .name("Rajesh Sharma")
                .contactNumber("9876543210")
                .relation("FATHER")
                .primaryGuardian(true)
                .whatsappEnabled(true)
                .build();

        StudentCreateRequest req1 = new StudentCreateRequest();
        req1.setAdmissionNumber("ADM-001");
        req1.setFirstName("Rahul");
        req1.setGender(Gender.MALE);
        req1.setGuardians(List.of(fatherReq));

        studentService.register(req1);

        // 2. Verify guardian was created
        List<Guardian> guardians = guardianRepository.findAll();
        assertThat(guardians).isNotEmpty();
        Guardian savedFather = guardians.stream()
                .filter(g -> g.getContactNumber().equals("9876543210"))
                .findFirst()
                .orElseThrow();

        // 3. Create second student (sibling) with same guardian contact
        StudentCreateRequest req2 = new StudentCreateRequest();
        req2.setAdmissionNumber("ADM-002");
        req2.setFirstName("Rohan");
        req2.setGender(Gender.MALE);
        req2.setGuardians(List.of(fatherReq));

        studentService.register(req2);

        // 4. Verify NO new guardian record was created for same contact
        long count = guardianRepository.findAll().stream()
                .filter(g -> g.getContactNumber().equals("9876543210"))
                .count();
        assertThat(count).isEqualTo(1);

        // 5. Verify both students linked to same guardian
        List<StudentGuardian> links = studentGuardianRepository.findByGuardianId(savedFather.getId());
        assertThat(links).hasSize(2);
    }

    @Test
    void testPrimaryGuardianValidation() {
        GuardianCreateRequest g1 = GuardianCreateRequest.builder()
                .name("G1")
                .contactNumber("111")
                .primaryGuardian(true)
                .build();

        GuardianCreateRequest g2 = GuardianCreateRequest.builder()
                .name("G2")
                .contactNumber("222")
                .primaryGuardian(true)
                .build();

        StudentCreateRequest req = new StudentCreateRequest();
        req.setAdmissionNumber("ADM-ERR");
        req.setFirstName("Error");
        req.setGender(Gender.MALE);
        req.setGuardians(List.of(g1, g2));

        assertThrows(IllegalArgumentException.class, () -> studentService.register(req));
    }

    private Long ensureSchoolId() {
        return schoolRepository.findBySchoolCode(TEST_SCHOOL_CODE)
                .map(School::getId)
                .orElseGet(() -> {
                    School s = School.builder()
                            .name("Guardian Test School")
                            .schoolCode(TEST_SCHOOL_CODE)
                            .active(true)
                            .build();
                    return schoolRepository.save(s).getId();
                });
    }

    private Long ensureSessionForSchool(Long schoolId) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow();

        Long currentSessionId = school.getCurrentSessionId();
        if (currentSessionId != null && sessionRepository.findById(currentSessionId).isPresent()) {
            return currentSessionId;
        }

        AcademicSession session = AcademicSession.builder()
                .name("TEST-" + System.nanoTime())
                .startDate(LocalDate.now().minusDays(1))
                .endDate(LocalDate.now().plusDays(365))
                .active(true)
                .build();
        session.setSchoolId(school.getId());
        AcademicSession savedSession = sessionRepository.save(session);

        school.setCurrentSessionId(savedSession.getId());
        schoolRepository.save(school);
        return savedSession.getId();
    }

    private void ensureClassExists(Long schoolId, Long sessionId) {
        if (schoolClassRepository.countBySchoolIdAndSessionId(schoolId, sessionId) > 0) {
            return;
        }

        SchoolClass schoolClass = SchoolClass.builder()
                .name("1")
                .section("A")
                .sessionId(sessionId)
                .active(true)
                .build();
        schoolClass.setSchoolId(schoolId);
        schoolClassRepository.save(schoolClass);
    }
}
