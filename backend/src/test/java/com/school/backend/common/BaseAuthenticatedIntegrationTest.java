package com.school.backend.common;

import com.school.backend.core.attendance.repository.AttendanceRepository;
import com.school.backend.core.classsubject.repository.SchoolClassRepository;
import com.school.backend.core.student.repository.PromotionRecordRepository;
import com.school.backend.core.student.repository.StudentEnrollmentRepository;
import com.school.backend.core.student.repository.StudentRepository;
import com.school.backend.core.teacher.repository.TeacherRepository;
import com.school.backend.fee.repository.FeePaymentRepository;
import com.school.backend.fee.repository.FeeStructureRepository;
import com.school.backend.fee.repository.FeeTypeRepository;
import com.school.backend.fee.repository.StudentFeeAssignmentRepository;
import com.school.backend.school.entity.AcademicSession;
import com.school.backend.school.entity.School;
import com.school.backend.school.repository.AcademicSessionRepository;
import com.school.backend.school.repository.SchoolRepository;
import com.school.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class BaseAuthenticatedIntegrationTest {

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected TestAuthHelper authHelper;

    @Autowired
    protected SchoolRepository schoolRepository;
    @Autowired
    protected AcademicSessionRepository sessionRepository;
    @Autowired
    protected UserRepository userRepository;
    @Autowired
    protected TeacherRepository teacherRepository;
    @Autowired
    protected SchoolClassRepository schoolClassRepository;
    @Autowired
    protected StudentRepository studentRepository;
    @Autowired
    protected StudentEnrollmentRepository studentEnrollmentRepository;
    @Autowired
    protected PromotionRecordRepository promotionRecordRepository;
    @Autowired
    protected AttendanceRepository attendanceRepository;
    @Autowired
    protected FeeTypeRepository feeTypeRepository;
    @Autowired
    protected FeeStructureRepository feeStructureRepository;
    @Autowired
    protected StudentFeeAssignmentRepository assignmentRepository;
    @Autowired
    protected FeePaymentRepository feePaymentRepository;

    protected String token;
    protected HttpHeaders headers;

    @BeforeEach
    void baseSetup() {
        loginAsSuperAdmin();
    }

    protected void loginAsSuperAdmin() {
        token = authHelper.createSuperAdminAndLogin();
        headers = authHelper.authHeaders(token);
    }

    protected void loginAsSchoolAdmin(Long schoolId) {
        token = authHelper.createSchoolAdminAndLogin(schoolId);
        headers = authHelper.authHeaders(token);
    }

    protected Long setupSession(Long schoolId, AcademicSessionRepository sessionRepository,
                                SchoolRepository schoolRepository) {
        AcademicSession session = sessionRepository
                .save(AcademicSession.builder()
                        .name("2025-26")
                        .schoolId(schoolId)
                        .active(true)
                        .build());

        School school = schoolRepository.findById(schoolId).orElseThrow();
        school.setCurrentSessionId(session.getId());
        schoolRepository.save(school);

        return session.getId();
    }

    protected void setSessionHeader(Long sessionId) {
        headers.set("X-Session-Id", String.valueOf(sessionId));
    }

    protected void fullCleanup() {
        attendanceRepository.deleteAll();
        promotionRecordRepository.deleteAll();
        studentEnrollmentRepository.deleteAll();
        feePaymentRepository.deleteAll();
        assignmentRepository.deleteAll();
        feeStructureRepository.deleteAll();
        feeTypeRepository.deleteAll();
        studentRepository.deleteAll();
        schoolClassRepository.deleteAll();
        teacherRepository.deleteAll();
        userRepository.deleteAll();
        sessionRepository.deleteAll();
        schoolRepository.deleteAll();
    }
}
