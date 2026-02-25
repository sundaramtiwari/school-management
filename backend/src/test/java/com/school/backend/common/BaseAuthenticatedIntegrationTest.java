package com.school.backend.common;

import com.school.backend.core.attendance.repository.AttendanceRepository;
import com.school.backend.core.classsubject.repository.ClassSubjectRepository;
import com.school.backend.core.classsubject.repository.SchoolClassRepository;
import com.school.backend.core.guardian.repository.GuardianRepository;
import com.school.backend.core.student.repository.PromotionRecordRepository;
import com.school.backend.core.student.repository.StudentEnrollmentRepository;
import com.school.backend.core.student.repository.StudentGuardianRepository;
import com.school.backend.core.student.repository.StudentRepository;
import com.school.backend.core.teacher.repository.TeacherRepository;
import com.school.backend.expense.repository.ExpenseHeadRepository;
import com.school.backend.expense.repository.ExpenseVoucherRepository;
import com.school.backend.fee.repository.FeePaymentRepository;
import com.school.backend.fee.repository.FeePaymentAllocationRepository;
import com.school.backend.fee.repository.LateFeePolicyRepository;
import com.school.backend.fee.repository.FeeStructureRepository;
import com.school.backend.fee.repository.FeeTypeRepository;
import com.school.backend.fee.repository.StudentFeeAssignmentRepository;
import com.school.backend.school.entity.AcademicSession;
import com.school.backend.school.entity.School;
import com.school.backend.school.repository.AcademicSessionRepository;
import com.school.backend.school.repository.SchoolRepository;
import com.school.backend.transport.repository.PickupPointRepository;
import com.school.backend.transport.repository.TransportEnrollmentRepository;
import com.school.backend.transport.repository.TransportRouteRepository;
import com.school.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import java.time.LocalDate;

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
    protected ClassSubjectRepository classSubjectRepository;
    @Autowired
    protected StudentRepository studentRepository;
    @Autowired
    protected StudentEnrollmentRepository studentEnrollmentRepository;
    @Autowired
    protected PromotionRecordRepository promotionRecordRepository;
    @Autowired
    protected AttendanceRepository attendanceRepository;
    @Autowired
    protected GuardianRepository guardianRepository;
    @Autowired
    protected StudentGuardianRepository studentGuardianRepository;
    @Autowired
    protected FeeTypeRepository feeTypeRepository;
    @Autowired
    protected FeeStructureRepository feeStructureRepository;
    @Autowired
    protected LateFeePolicyRepository lateFeePolicyRepository;
    @Autowired
    protected StudentFeeAssignmentRepository assignmentRepository;
    @Autowired
    protected FeePaymentRepository feePaymentRepository;
    @Autowired
    protected FeePaymentAllocationRepository feePaymentAllocationRepository;
    @Autowired
    protected TransportEnrollmentRepository transportEnrollmentRepository;
    @Autowired
    protected PickupPointRepository pickupPointRepository;
    @Autowired
    protected TransportRouteRepository transportRouteRepository;
    @Autowired
    protected ExpenseHeadRepository expenseHeadRepository;
    @Autowired
    protected ExpenseVoucherRepository expenseVoucherRepository;

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
                        .startDate(LocalDate.of(2025, 4, 1))
                        .endDate(LocalDate.of(2026, 3, 31))
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
        transportEnrollmentRepository.deleteAll();
        pickupPointRepository.deleteAll();
        transportRouteRepository.deleteAll();
        attendanceRepository.deleteAll();
        promotionRecordRepository.deleteAll();
        studentEnrollmentRepository.deleteAll();
        studentGuardianRepository.deleteAll();
        guardianRepository.deleteAll();
        expenseVoucherRepository.deleteAll();
        expenseHeadRepository.deleteAll();
        feePaymentAllocationRepository.deleteAll();
        feePaymentRepository.deleteAll();
        assignmentRepository.deleteAll();
        lateFeePolicyRepository.deleteAll();
        feeStructureRepository.deleteAll();
        feeTypeRepository.deleteAll();
        studentRepository.deleteAll();
        classSubjectRepository.deleteAll();
        schoolClassRepository.deleteAll();
        teacherRepository.deleteAll();
        userRepository.deleteAll();
        sessionRepository.deleteAll();
        schoolRepository.deleteAll();
    }
}
