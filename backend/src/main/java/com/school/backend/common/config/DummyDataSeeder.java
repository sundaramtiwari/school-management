package com.school.backend.common.config;

import com.school.backend.common.enums.Gender;
import com.school.backend.common.enums.UserRole;
import com.school.backend.core.attendance.entity.StudentAttendance;
import com.school.backend.core.attendance.enums.AttendanceStatus;
import com.school.backend.core.attendance.repository.AttendanceRepository;
import com.school.backend.core.classsubject.entity.SchoolClass;
import com.school.backend.core.classsubject.repository.SchoolClassRepository;
import com.school.backend.core.student.entity.Student;
import com.school.backend.core.student.repository.StudentRepository;
import com.school.backend.core.teacher.entity.Teacher;
import com.school.backend.core.teacher.repository.TeacherRepository;
import com.school.backend.fee.entity.FeeStructure;
import com.school.backend.fee.entity.FeeType;
import com.school.backend.fee.entity.StudentFeeAssignment;
import com.school.backend.fee.enums.FeeFrequency;
import com.school.backend.fee.repository.FeeStructureRepository;
import com.school.backend.fee.repository.FeeTypeRepository;
import com.school.backend.fee.repository.StudentFeeAssignmentRepository;
import com.school.backend.school.entity.School;
import com.school.backend.school.repository.SchoolRepository;
import com.school.backend.transport.entity.PickupPoint;
import com.school.backend.transport.entity.TransportEnrollment;
import com.school.backend.transport.entity.TransportRoute;
import com.school.backend.transport.repository.PickupPointRepository;
import com.school.backend.transport.repository.TransportEnrollmentRepository;
import com.school.backend.transport.repository.TransportRouteRepository;
import com.school.backend.user.entity.User;
import com.school.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
@Profile("!prod")
@RequiredArgsConstructor
@Slf4j
public class DummyDataSeeder implements CommandLineRunner {

    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final TeacherRepository teacherRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final StudentRepository studentRepository;
    private final FeeTypeRepository feeTypeRepository;
    private final FeeStructureRepository feeStructureRepository;
    private final StudentFeeAssignmentRepository studentFeeAssignmentRepository;
    private final TransportRouteRepository transportRouteRepository;
    private final PickupPointRepository pickupPointRepository;
    private final TransportEnrollmentRepository transportEnrollmentRepository;
    private final AttendanceRepository attendanceRepository;
    private final com.school.backend.school.repository.AcademicSessionRepository academicSessionRepository;

    private final PasswordEncoder passwordEncoder;
    private final Random random = new Random();

    @Override
    @Transactional
    public void run(String... args) {
//                if (schoolRepository.count() > 1) {
//                        log.info("Schools already exist. Skipping dummy data seeding.");
//                        return;
//                }
//                log.info("Starting Dummy Data Seeding...");
//
//                for (int i = 1; i <= 5; i++) {
//                        seedSchool(i);
//                }
//
//                log.info("Dummy Data Seeding Completed!");
    }

    private void seedSchool(int index) {
        String schoolName = "School " + index;
        String schoolCode = "SCH" + String.format("%03d", index);

        // 1. Create School
        School school = School.builder()
                .name(schoolName)
                .schoolCode(schoolCode)
                .displayName(schoolName + " International")
                .board("CBSE")
                .medium("English")
                .contactEmail("admin@" + schoolCode.toLowerCase() + ".com")
                .address("Address for " + schoolName)
                .city("City " + index)
                .active(true)
                .build();
        school = schoolRepository.save(school);
        log.info("Created School: {}", school.getName());

        // 2. Create School Admin
        User schoolAdmin = User.builder()
                .email("admin_" + schoolCode.toLowerCase() + "@test.com")
                .passwordHash(passwordEncoder.encode("password"))
                .fullName("Admin " + schoolName)
                .role(UserRole.SCHOOL_ADMIN)
                .school(school)
                .active(true)
                .build();
        userRepository.save(schoolAdmin);

        // 3. Create Teachers
        List<Teacher> teachers = new ArrayList<>();
        for (int i = 1; i <= 8; i++) {
            User teacherUser = User.builder()
                    .email("teacher" + i + "_" + schoolCode.toLowerCase() + "@test.com")
                    .passwordHash(passwordEncoder.encode("password"))
                    .fullName("Teacher " + i + " " + schoolName)
                    .role(UserRole.TEACHER)
                    .school(school)
                    .active(true)
                    .build();
            teacherUser = userRepository.save(teacherUser);

            Teacher teacher = Teacher.builder()
                    .user(teacherUser)
                    .build();
            teacher.setSchoolId(school.getId());
            teachers.add(teacherRepository.save(teacher));
        }

        // 4. Create Fee Types
        FeeType tuitionFee = FeeType.builder().name("Tuition Fee " + schoolCode).active(true).build();
        tuitionFee.setSchoolId(school.getId());
        tuitionFee = feeTypeRepository.save(tuitionFee);

        FeeType transportFee = FeeType.builder().name("Transport Fee " + schoolCode).active(true).build();
        transportFee.setSchoolId(school.getId());
        transportFee = feeTypeRepository.save(transportFee);

        // Create Academic Session
        com.school.backend.school.entity.AcademicSession session = com.school.backend.school.entity.AcademicSession
                .builder()
                .name("2025-26")
                .active(true)
                .build();
        session.setSchoolId(school.getId());
        session = academicSessionRepository.save(session);

        String[] classNames = {"Nursery", "LKG", "UKG", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10",
                "11",
                "12"};
        List<SchoolClass> classes = new ArrayList<>();

        for (int i = 0; i < classNames.length; i++) {
            String className = classNames[i];
            SchoolClass sClass = SchoolClass.builder()
                    .name(className)
                    .sessionId(session.getId())
                    .section("A")
                    .capacity(40)
                    .classTeacher(teachers.get(i % teachers.size())) // distribute teachers
                    .active(true)
                    .build();
            sClass.setSchoolId(school.getId());
            sClass = schoolClassRepository.save(sClass);
            classes.add(sClass);

            // Fee Structure
            FeeStructure fs = FeeStructure.builder()
                    .classId(sClass.getId())
                    .feeType(tuitionFee)
                    .amount(5000 + (i * 500)) // varying fee
                    .frequency(FeeFrequency.MONTHLY)
                    .sessionId(session.getId())
                    .active(true)
                    .build();
            fs.setSchoolId(school.getId());
            feeStructureRepository.save(fs);
        }

        // 6. Transport Routes
        TransportRoute route1 = TransportRoute.builder()
                .name("Route A " + schoolCode)
                .capacity(50)
                .currentStrength(0)
                .active(true)
                .build();
        route1.setSchoolId(school.getId());
        route1 = transportRouteRepository.save(route1);

        PickupPoint p1 = PickupPoint.builder()
                .name("Stop 1")
                .amount(1000)
                .frequency(FeeFrequency.MONTHLY)
                .route(route1)
                .build();
        p1.setSchoolId(school.getId());
        p1 = pickupPointRepository.save(p1);

        // 7. Students, Fee Assignments, Transport Enrollment, Attendance
        for (SchoolClass sClass : classes) {
            int studentsInClass = 15 + random.nextInt(10); // 15-25 students per class
            for (int k = 1; k <= studentsInClass; k++) {
                String admNo = "ADM" + schoolCode + sClass.getName() + k;
                Student student = Student.builder()
                        .firstName("Student " + k)
                        .lastName(sClass.getName())
                        .admissionNumber(admNo)
                        .gender(k % 2 == 0 ? Gender.MALE : Gender.FEMALE)
                        .dob(LocalDate.of(2010 + (classes.indexOf(sClass) > 10 ? 0 : 5), 1, 1))
                        .currentClass(sClass)
                        .active(true)
                        .currentStatus("ENROLLED")
                        .build();
                student.setSchoolId(school.getId());
                student = studentRepository.save(student);

                // Assign Tuition Fee
                // Look up fee structure for this class
                // Simpler: just get all fee structures for this class (we created one)
                // In real app, we'd query repository. Here we can't easily.
                // Re-fetch or simplistic assumption.
                // Let's create assumption: we know we created a fee structure for this class.
                // We'll skip complex assignment logic and just create a record if we can.
                // Actually, FeeAssignment links student to FeeStructure.
                // We need the FeeStructure ID.
                // Optimization: Store FeeStructures in a map or list parallel to classes.
                // For simplicity, I'll fetch it or just create a new one? No, must match.
                // I'll skip FeeAssignment for now to avoid complexity in this loop, or fetch
                // it.
                // Improved: Let's fetch FeeStructure by classId
                List<FeeStructure> classFeeStructures = feeStructureRepository
                        .findByClassIdAndSessionIdAndSchoolId(sClass.getId(), session.getId(),
                                school.getId());
                for (FeeStructure fs : classFeeStructures) {
                    int finalAmount = fs.getAmount();
                    if (fs.getFrequency() == FeeFrequency.MONTHLY) {
                        finalAmount = finalAmount * 12;
                    }
                    StudentFeeAssignment sfa = StudentFeeAssignment.builder()
                            .studentId(student.getId())
                            .feeStructureId(fs.getId())
                            .sessionId(session.getId())
                            .amount(finalAmount)
                            .active(true)
                            .build();
                    sfa.setSchoolId(school.getId());
                    studentFeeAssignmentRepository.save(sfa);
                }

                // Transport Enrollment (20% chance)
                if (random.nextInt(5) == 0) {
                    TransportEnrollment te = TransportEnrollment.builder()
                            .studentId(student.getId())
                            .pickupPoint(p1)
                            .sessionId(session.getId())
                            .active(true)
                            .build();
                    te.setSchoolId(school.getId());
                    transportEnrollmentRepository.save(te);

                    // Update route strength? Skipping for now.
                }

                // Attendance (Last 7 days)
                LocalDate today = LocalDate.now();
                for (int d = 0; d < 7; d++) {
                    LocalDate date = today.minusDays(d);
                    // Skip Sundays
                    if (date.getDayOfWeek().getValue() == 7)
                        continue;

                    StudentAttendance attendance = StudentAttendance.builder()
                            .studentId(student.getId())
                            .attendanceDate(date)
                            .status(random.nextInt(10) > 1 ? AttendanceStatus.PRESENT
                                    : AttendanceStatus.ABSENT)
                            .build();
                    attendance.setSchoolId(school.getId());
                    attendanceRepository.save(attendance);
                }
            }
        }
    }
}
