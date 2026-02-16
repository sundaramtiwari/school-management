package com.school.backend.common.config;

import com.school.backend.common.enums.Gender;
import com.school.backend.common.enums.UserRole;
import com.school.backend.common.tenant.TenantContext;
import com.school.backend.core.classsubject.dto.ClassSubjectDto;
import com.school.backend.core.classsubject.dto.SchoolClassDto;
import com.school.backend.core.classsubject.dto.SubjectDto;
import com.school.backend.core.classsubject.service.ClassSubjectService;
import com.school.backend.core.classsubject.service.SchoolClassService;
import com.school.backend.core.classsubject.service.SubjectService;
import com.school.backend.core.student.dto.StudentCreateRequest;
import com.school.backend.core.student.dto.StudentEnrollmentRequest;
import com.school.backend.core.student.service.EnrollmentService;
import com.school.backend.core.student.service.StudentService;
import com.school.backend.fee.dto.FeePaymentRequest;
import com.school.backend.fee.dto.FeeStructureCreateRequest;
import com.school.backend.fee.enums.FeeFrequency;
import com.school.backend.fee.repository.FeeStructureRepository;
import com.school.backend.fee.service.FeePaymentService;
import com.school.backend.fee.service.FeeStructureService;
import com.school.backend.fee.service.FeeTypeService;
import com.school.backend.school.dto.SchoolOnboardingRequest;
import com.school.backend.school.entity.AcademicSession;
import com.school.backend.school.entity.School;
import com.school.backend.school.repository.SchoolRepository;
import com.school.backend.school.service.AcademicSessionService;
import com.school.backend.school.service.SchoolService;
import com.school.backend.testmanagement.dto.ExamCreateRequest;
import com.school.backend.testmanagement.dto.ExamSubjectCreateRequest;
import com.school.backend.testmanagement.entity.Exam;
import com.school.backend.testmanagement.service.ExamService;
import com.school.backend.testmanagement.service.ExamSubjectService;
import com.school.backend.user.entity.User;
import com.school.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Demo data dependencies
    private final SchoolService schoolService;
    private final AcademicSessionService academicSessionService;
    private final SchoolRepository schoolRepository;
    private final SchoolClassService schoolClassService;
    private final StudentService studentService;
    private final EnrollmentService enrollmentService;
    private final FeeStructureService feeStructureService;
    private final FeePaymentService feePaymentService;
    private final FeeTypeService feeTypeService;
    private final FeeStructureRepository feeStructureRepository;
    private final SubjectService subjectService;
    private final ClassSubjectService classSubjectService;
    private final ExamService examService;
    private final ExamSubjectService examSubjectService;
    private final Environment environment;

    private static @NonNull SchoolOnboardingRequest getSchoolOnboardingRequest(String demoCode) {
        SchoolOnboardingRequest req = new SchoolOnboardingRequest();
        req.setName("Glorious Public School");
        req.setDisplayName("Glorious Public School");
        req.setBoard("CBSE");
        req.setMedium("English");
        req.setSchoolCode(demoCode);
        req.setCity("Pune");
        req.setState("Maharashtra");
        req.setContactEmail("demo.school@example.com");
        req.setContactNumber("9999999999");
        req.setAddress("Demo Street, Pune");
        req.setPincode("411001");
        req.setWebsite("https://demo-school.local");
        req.setDescription("Demo school for local development data");
        req.setAdminEmail("admin1@school.com");
        req.setAdminPassword("admin1");
        req.setAdminName("Demo Admin");
        return req;
    }

    @Override
    public void run(String... args) {
        createSuperAdminIfMissing();

        // Seed rich demo data only on dev-like profiles
        if (!isDevProfile()) {
            return;
        }

        seedDemoAcademicAndFeeData();
    }

    private void createSuperAdminIfMissing() {
        if (userRepository.existsByEmail("admin@school.com")) {
            return;
        }

        User admin = User.builder()
                .email("admin@school.com")
                .passwordHash(passwordEncoder.encode("admin"))
                .role(UserRole.SUPER_ADMIN)
                .active(true)
                .build();

        userRepository.save(admin);
        System.out.println("CREATED SUPER ADMIN: admin@school.com / admin");
    }

    private boolean isDevProfile() {
        String[] profiles = environment.getActiveProfiles();
        return Arrays.stream(profiles).anyMatch(p -> p.equalsIgnoreCase("dev") || p.equalsIgnoreCase("local"));
    }

    private void seedDemoAcademicAndFeeData() {
        Long previousTenant = TenantContext.getSchoolId();
        try {
            School demoSchool = createOrGetDemoSchool();
            if (demoSchool == null) {
                return;
            }

            AcademicSession session = createOrGetDemoSession(demoSchool);
            if (session == null) {
                return;
            }

            TenantContext.setSchoolId(demoSchool.getId());

            seedClasses(demoSchool, session);
            seedStudentsAndEnrollments(demoSchool, session);
            seedFeeTypesAndStructures(demoSchool, session);
            seedFeePayments(demoSchool, session);
            seedSubjectsAndClassSubjects(demoSchool, session);
            seedExams(demoSchool, session);
        } finally {
            if (previousTenant != null) {
                TenantContext.setSchoolId(previousTenant);
            } else {
                TenantContext.clear();
            }
        }
    }

    private School createOrGetDemoSchool() {
        String demoCode = "DEMO";

        Optional<School> existing = schoolRepository.findBySchoolCode(demoCode);
        if (existing.isPresent()) {
            return existing.get();
        }

        SchoolOnboardingRequest req = getSchoolOnboardingRequest(demoCode);

        try {
            schoolService.createSchoolWithAdmin(req);
        } catch (Exception ex) {
            // If something went wrong (e.g. user already exists), try to recover by fetching the school
            System.out.println("Skipping demo school creation: " + ex.getMessage());
        }

        return schoolRepository.findBySchoolCode(demoCode).orElse(null);
    }

    private AcademicSession createOrGetDemoSession(School school) {
        Long schoolId = school.getId();
        String sessionName = "2024-25";

        // Try to find existing active session with the same name
        List<AcademicSession> sessions = academicSessionService.getSessions(schoolId);
        for (AcademicSession s : sessions) {
            if (sessionName.equals(s.getName())) {
                return s;
            }
        }

        AcademicSession session = new AcademicSession();
        session.setSchoolId(schoolId);
        session.setName(sessionName);
        session.setActive(true);

        return academicSessionService.createSession(session);
    }

    private void seedClasses(School school, AcademicSession session) {
        // Two demo classes: Class 1 - A and Class 2 - A
        Long sessionId = session.getId();

        createClassIfMissing("Class 1", "A", sessionId, 40);
        createClassIfMissing("Class 2", "A", sessionId, 40);
    }

    private void createClassIfMissing(String name, String section, Long sessionId, int capacity) {
        SchoolClassDto dto = new SchoolClassDto();
        dto.setName(name);
        dto.setSection(section);
        dto.setSessionId(sessionId);
        dto.setCapacity(capacity);
        dto.setActive(true);

        try {
            schoolClassService.create(dto);
        } catch (IllegalArgumentException ex) {
            // Most likely duplicate; safe to ignore for idempotent seeding
            System.out.println("Skipping class creation for " + name + " - " + section + ": " + ex.getMessage());
        }
    }

    private void seedStudentsAndEnrollments(School school, AcademicSession session) {
        Long schoolId = school.getId();
        Long sessionId = session.getId();

        // We assume classes were created in seedClasses; fetch them via service
        List<SchoolClassDto> classes = schoolClassService
                .getBySchoolAndSession(schoolId, sessionId,
                        org.springframework.data.domain.PageRequest.of(0, 10))
                .getContent();

        if (classes.isEmpty()) {
            return;
        }

        // Create a couple of students and enroll them into the first class
        SchoolClassDto targetClass = classes.get(0);

        createStudentAndEnrollIfMissing("ADM001", "Rohan", "Sharma", Gender.MALE, targetClass, sessionId, 1);
        createStudentAndEnrollIfMissing("ADM002", "Priya", "Verma", Gender.FEMALE, targetClass, sessionId, 2);
        createStudentAndEnrollIfMissing("ADM003", "Aarav", "Patel", Gender.MALE, targetClass, sessionId, 3);
    }

    private void createStudentAndEnrollIfMissing(
            String admissionNumber,
            String firstName,
            String lastName,
            Gender gender,
            SchoolClassDto schoolClass,
            Long sessionId,
            int rollNumber
    ) {
        Long schoolId = TenantContext.getSchoolId();

        if (schoolId == null) {
            return;
        }

        if (studentServiceExists(admissionNumber, schoolId)) {
            return;
        }

        StudentCreateRequest createRequest = new StudentCreateRequest();
        createRequest.setAdmissionNumber(admissionNumber);
        createRequest.setFirstName(firstName);
        createRequest.setLastName(lastName);
        createRequest.setGender(gender);
        createRequest.setCity("Pune");
        createRequest.setState("Maharashtra");
        createRequest.setDateOfAdmission(LocalDate.now());
        createRequest.setSchoolId(schoolId);

        Long studentId;
        try {
            studentId = studentService.register(createRequest).getId();
        } catch (Exception ex) {
            System.out.println("Skipping student creation for " + admissionNumber + ": " + ex.getMessage());
            return;
        }

        StudentEnrollmentRequest enrollmentRequest = new StudentEnrollmentRequest();
        enrollmentRequest.setStudentId(studentId);
        enrollmentRequest.setClassId(schoolClass.getId());
        enrollmentRequest.setSessionId(sessionId);
        enrollmentRequest.setRollNumber(rollNumber);
        enrollmentRequest.setEnrollmentDate(LocalDate.now());

        try {
            enrollmentService.enroll(enrollmentRequest);
        } catch (Exception ex) {
            System.out.println("Skipping enrollment for " + admissionNumber + ": " + ex.getMessage());
        }
    }

    private boolean studentServiceExists(String admissionNumber, Long schoolId) {
        // Wrapper so we can keep DataSeeder independent of StudentRepository
        try {
            return studentService
                    .listBySchool(schoolId, org.springframework.data.domain.PageRequest.of(0, 1))
                    .stream()
                    .anyMatch(s -> admissionNumber.equals(s.getAdmissionNumber()));
        } catch (Exception ex) {
            return false;
        }
    }

    private void seedFeeTypesAndStructures(School school, AcademicSession session) {
        Long schoolId = school.getId();
        Long sessionId = session.getId();

        // Ensure base fee types (tenant-scoped via TenantContext)
        createFeeTypeIfMissing("Tuition Fee");
        createFeeTypeIfMissing("Transport Fee");

        // Create a tuition fee structure for each class if not already present
        var classesPage = schoolClassService.getBySchoolAndSession(
                schoolId,
                sessionId,
                org.springframework.data.domain.PageRequest.of(0, 20)
        );

        classesPage.getContent().forEach(c -> {
            var existing = feeStructureRepository.findByClassIdAndSessionIdAndSchoolId(
                    c.getId(),
                    sessionId,
                    schoolId
            );

            if (!existing.isEmpty()) {
                return;
            }

            Long tuitionFeeTypeId = feeTypeService.list().stream()
                    .filter(ft -> "Tuition Fee".equalsIgnoreCase(ft.getName()))
                    .findFirst()
                    .map(com.school.backend.fee.entity.FeeType::getId)
                    .orElse(null);

            if (tuitionFeeTypeId == null) {
                return;
            }

            FeeStructureCreateRequest req = new FeeStructureCreateRequest();
            req.setClassId(c.getId());
            req.setSessionId(sessionId);
            req.setFeeTypeId(tuitionFeeTypeId);
            req.setAmount(20000);
            req.setFrequency(FeeFrequency.ANNUALLY);

            try {
                feeStructureService.create(req);
            } catch (Exception ex) {
                System.out.println("Skipping fee structure for class " + c.getName() + ": " + ex.getMessage());
            }
        });
    }

    private void createFeeTypeIfMissing(String name) {
        Long schoolId = TenantContext.getSchoolId();
        if (schoolId == null) {
            return;
        }

        // Check only within current tenant's fee types
        try {
            boolean exists = feeTypeService.list().stream()
                    .anyMatch(ft -> name.equalsIgnoreCase(ft.getName()));
            if (exists) {
                return;
            }
        } catch (Exception ignored) {
        }

        com.school.backend.fee.entity.FeeType type = new com.school.backend.fee.entity.FeeType();
        type.setName(name);
        type.setDescription(name + " (demo)");

        try {
            feeTypeService.create(type);
        } catch (Exception ex) {
            System.out.println("Skipping fee type creation for " + name + ": " + ex.getMessage());
        }
    }

    private void seedFeePayments(School school, AcademicSession session) {
        Long schoolId = school.getId();
        Long sessionId = session.getId();

        var studentsPage = studentService.listBySchool(
                schoolId,
                org.springframework.data.domain.PageRequest.of(0, 5)
        );

        if (studentsPage.isEmpty()) {
            return;
        }

        studentsPage.getContent().stream().limit(2).forEach(s -> {
            FeePaymentRequest req = new FeePaymentRequest();
            req.setStudentId(s.getId());
            req.setSessionId(sessionId);
            req.setAmountPaid(5000);
            req.setPaymentDate(LocalDate.now());
            req.setMode("CASH");
            req.setRemarks("Initial installment (demo)");

            try {
                feePaymentService.pay(req);
            } catch (Exception ex) {
                System.out.println("Skipping fee payment for student " + s.getId() + ": " + ex.getMessage());
            }
        });
    }

    private void seedSubjectsAndClassSubjects(School school, AcademicSession session) {
        Long schoolId = school.getId();
        Long sessionId = session.getId();

        // Create a small set of subjects
        SubjectDto math = createSubjectIfMissing("Mathematics", "MATH");
        SubjectDto science = createSubjectIfMissing("Science", "SCI");
        SubjectDto english = createSubjectIfMissing("English", "ENG");

        var subjects = List.of(math, science, english);

        var classesPage = schoolClassService.getBySchoolAndSession(
                schoolId,
                sessionId,
                org.springframework.data.domain.PageRequest.of(0, 20)
        );

        classesPage.getContent().forEach(c -> {
            int order = 1;
            for (SubjectDto subj : subjects) {
                if (subj == null || subj.getId() == null) {
                    continue;
                }

                ClassSubjectDto dto = new ClassSubjectDto();
                dto.setClassId(c.getId());
                dto.setSubjectId(subj.getId());
                dto.setDisplayOrder(order++);
                dto.setActive(true);

                try {
                    classSubjectService.create(dto);
                } catch (Exception ex) {
                    System.out.println("Skipping class-subject for class " + c.getId() + " and subject " + subj.getName()
                            + ": " + ex.getMessage());
                }
            }
        });
    }

    private SubjectDto createSubjectIfMissing(String name, String code) {
        Long schoolId = TenantContext.getSchoolId();
        if (schoolId == null) {
            return null;
        }

        try {
            // Use paging to search for existing subject by name
            var page = subjectService.getBySchool(
                    schoolId,
                    true,
                    org.springframework.data.domain.PageRequest.of(0, 50)
            );
            var existing = page.getContent().stream()
                    .filter(s -> name.equalsIgnoreCase(s.getName()))
                    .findFirst();
            if (existing.isPresent()) {
                return existing.get();
            }
        } catch (Exception ignored) {
        }

        SubjectDto dto = new SubjectDto();
        dto.setName(name);
        dto.setCode(code);
        dto.setType("THEORY");
        dto.setMaxMarks(100);
        dto.setMinMarks(33);
        dto.setActive(true);
        dto.setRemarks("Demo subject");

        try {
            return subjectService.create(dto);
        } catch (Exception ex) {
            System.out.println("Skipping subject creation for " + name + ": " + ex.getMessage());
            return null;
        }
    }

    private void seedExams(School school, AcademicSession session) {
        Long schoolId = school.getId();
        Long sessionId = session.getId();

        var classesPage = schoolClassService.getBySchoolAndSession(
                schoolId,
                sessionId,
                org.springframework.data.domain.PageRequest.of(0, 10)
        );

        if (classesPage.isEmpty()) {
            return;
        }

        SchoolClassDto targetClass = classesPage.getContent().get(0);

        Exam unitTest = createExamIfMissing(schoolId, sessionId, targetClass.getId(), "Unit Test 1", "UNIT_TEST");
        Exam halfYearly = createExamIfMissing(schoolId, sessionId, targetClass.getId(), "Half Yearly", "TERM");

        attachSubjectsToExam(unitTest);
        attachSubjectsToExam(halfYearly);
    }

    private Exam createExamIfMissing(Long schoolId, Long sessionId, Long classId, String name, String examType) {
        // Use listByClass to check existing exams by name
        try {
            List<Exam> existing = examService.listByClass(classId, sessionId);
            for (Exam e : existing) {
                if (name.equalsIgnoreCase(e.getName())) {
                    return e;
                }
            }
        } catch (Exception ignored) {
        }

        ExamCreateRequest req = new ExamCreateRequest();
        req.setSchoolId(schoolId);
        req.setClassId(classId);
        req.setSessionId(sessionId);
        req.setName(name);
        req.setExamType(examType);

        try {
            return examService.create(req);
        } catch (Exception ex) {
            System.out.println("Skipping exam creation for " + name + ": " + ex.getMessage());
            return null;
        }
    }

    private void attachSubjectsToExam(Exam exam) {
        if (exam == null) {
            return;
        }

        Long examId = exam.getId();

        // Attach all active subjects for the current school to this exam
        Long schoolId = TenantContext.getSchoolId();
        if (schoolId == null) {
            return;
        }

        var activeSubjects = subjectService.getBySchool(
                schoolId,
                true,
                org.springframework.data.domain.PageRequest.of(0, 50)
        );

        activeSubjects.getContent().forEach(s -> {
            ExamSubjectCreateRequest req = new ExamSubjectCreateRequest();
            req.setExamId(examId);
            req.setSubjectId(s.getId());
            req.setMaxMarks(100);

            try {
                examSubjectService.create(req);
            } catch (Exception ex) {
                System.out.println("Skipping exam-subject for exam " + examId + " and subject " + s.getId()
                        + ": " + ex.getMessage());
            }
        });
    }
}
