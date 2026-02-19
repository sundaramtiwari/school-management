package com.school.backend.devtools.seeder;

import com.school.backend.common.enums.Gender;
import com.school.backend.core.classsubject.entity.SchoolClass;
import com.school.backend.core.student.entity.PromotionRecord;
import com.school.backend.core.student.entity.Student;
import com.school.backend.core.student.entity.StudentEnrollment;
import com.school.backend.core.student.repository.PromotionRecordRepository;
import com.school.backend.core.student.repository.StudentEnrollmentRepository;
import com.school.backend.core.student.repository.StudentRepository;
import com.school.backend.school.entity.School;
import com.school.backend.student.enums.AdmissionType;
import com.school.backend.student.enums.PromotionType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class StudentSeeder {

    private static final int STUDENTS_PER_SCHOOL = 500;
    private static final int CURRENT_ONLY_COUNT = 200;
    private static final int PAST_ONLY_COUNT = 150;
    private static final int PROMOTED_COUNT = 150;
    private static final int REPEAT_COUNT = 50;
    private static final int FAIL_COUNT = 50;
    private static final int SAME_SESSION_PROMOTION_COUNT = 20;

    private static final List<String> CLASS_FLOW = List.of(
            "Nursery", "LKG", "UKG", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12"
    );

    private final StudentRepository studentRepository;
    private final StudentEnrollmentRepository studentEnrollmentRepository;
    private final PromotionRecordRepository promotionRecordRepository;

    @Transactional
    public Result seed(
            Random random,
            SchoolSeeder.Result schoolResult,
            SessionSeeder.Result sessionResult,
            ClassSubjectSeeder.Result classSubjectResult
    ) {
        List<Student> studentsToSave = new ArrayList<>(schoolResult.schools().size() * STUDENTS_PER_SCHOOL);
        List<StudentPlan> plans = new ArrayList<>(schoolResult.schools().size() * STUDENTS_PER_SCHOOL);

        String[] firstNames = {
                "Aarav", "Vivaan", "Aditya", "Vihaan", "Arjun", "Sai", "Reyansh", "Krishna", "Ishaan", "Atharv",
                "Aadhya", "Ananya", "Diya", "Myra", "Sara", "Kiara", "Anika", "Ira", "Pari", "Siya"
        };
        String[] lastNames = {
                "Sharma", "Verma", "Gupta", "Singh", "Kumar", "Mehta", "Jain", "Agarwal", "Yadav", "Joshi"
        };

        for (School school : schoolResult.schools()) {
            SessionSeeder.SessionTriplet sessions = sessionResult.sessionsBySchool().get(school.getId());
            Map<String, SchoolClass> pastClasses = classSubjectResult.classBySchoolSessionName()
                    .get(school.getId())
                    .get(sessions.completed().getId());
            Map<String, SchoolClass> currentClasses = classSubjectResult.classBySchoolSessionName()
                    .get(school.getId())
                    .get(sessions.active().getId());

            for (int index = 0; index < STUDENTS_PER_SCHOOL; index++) {
                String admissionNumber = String.format("%s-ADM-%04d", school.getSchoolCode(), index + 1);
                String firstName = firstNames[index % firstNames.length];
                String lastName = lastNames[(index / 3) % lastNames.length];
                Gender gender = (index % 2 == 0) ? Gender.MALE : Gender.FEMALE;
                LocalDate dob = LocalDate.of(2008 + (index % 10), (index % 12) + 1, ((index % 27) + 1));

                StudentType type = typeForIndex(index);
                String baseClass = CLASS_FLOW.get(index % CLASS_FLOW.size());
                String nextClass = nextClassName(baseClass);

                SchoolClass pastClass = pastClasses.get(baseClass);
                SchoolClass currentClass = switch (type) {
                    case CURRENT_ONLY -> currentClasses.get(baseClass);
                    case PAST_ONLY -> pastClass;
                    case PROMOTED -> currentClasses.get(index < (CURRENT_ONLY_COUNT + PAST_ONLY_COUNT + REPEAT_COUNT)
                            ? baseClass
                            : nextClass);
                };

                String status = switch (type) {
                    case PAST_ONLY -> (index < CURRENT_ONLY_COUNT + FAIL_COUNT ? "FAILED" : "LEFT");
                    case CURRENT_ONLY, PROMOTED -> "ENROLLED";
                };

                Student student = Student.builder()
                        .schoolId(school.getId())
                        .admissionNumber(admissionNumber)
                        .firstName(firstName)
                        .lastName(lastName)
                        .gender(gender)
                        .dob(dob)
                        .dateOfAdmission(LocalDate.of(2023, 4, 1).plusDays(index % 90))
                        .address("Block " + ((index % 20) + 1) + ", " + school.getCity())
                        .city(school.getCity())
                        .state(school.getState())
                        .pincode(school.getPincode())
                        .contactNumber(String.format("9%09d", school.getId() * 1000 + index))
                        .active(true)
                        .currentStatus(status)
                        .currentClass(currentClass)
                        .remarks("Seeded student")
                        .build();

                studentsToSave.add(student);
                plans.add(new StudentPlan(student, type, pastClass, currentClass, sessions));
            }
        }

        List<Student> savedStudents = studentRepository.saveAll(studentsToSave);

        List<StudentEnrollment> enrollmentsToSave = new ArrayList<>(savedStudents.size() * 2);
        List<PromotionRecord> promotionsToSave = new ArrayList<>();

        Map<Long, Integer> rollByClassSession = new LinkedHashMap<>();
        int sameSessionPromotionCounter = 0;

        for (StudentPlan plan : plans) {
            Student student = plan.student();
            SessionSeeder.SessionTriplet sessions = plan.sessions();

            if (plan.type() == StudentType.CURRENT_ONLY) {
                enrollmentsToSave.add(buildEnrollment(
                        student, plan.currentClass().getId(), sessions.active().getId(), AdmissionType.PROMOTION, rollByClassSession
                ));
            } else if (plan.type() == StudentType.PAST_ONLY) {
                enrollmentsToSave.add(buildEnrollment(
                        student, plan.pastClass().getId(), sessions.completed().getId(), AdmissionType.PROMOTION, rollByClassSession
                ));
            } else {
                enrollmentsToSave.add(buildEnrollment(
                        student, plan.pastClass().getId(), sessions.completed().getId(), AdmissionType.PROMOTION, rollByClassSession
                ));
                boolean repeat = plan.currentClass().getName().equals(plan.pastClass().getName());
                enrollmentsToSave.add(buildEnrollment(
                        student,
                        plan.currentClass().getId(),
                        sessions.active().getId(),
                        repeat ? AdmissionType.REPEAT : AdmissionType.PROMOTION,
                        rollByClassSession
                ));

                promotionsToSave.add(
                        PromotionRecord.builder()
                                .schoolId(student.getSchoolId())
                                .studentId(student.getId())
                                .sourceSessionId(sessions.completed().getId())
                                .targetSessionId(sessions.active().getId())
                                .sourceClassId(plan.pastClass().getId())
                                .targetClassId(plan.currentClass().getId())
                                .promotionType(repeat ? PromotionType.REPEAT : PromotionType.PROMOTE)
                                .promotedBy("SYSTEM_SEEDER")
                                .promotedAt(LocalDateTime.of(2024, 4, 5, 9, 0).plusSeconds(student.getId() % 60))
                                .remarks(repeat ? "Repeat due to performance" : "Regular promotion")
                                .build()
                );
            }

            if (plan.type() == StudentType.CURRENT_ONLY && sameSessionPromotionCounter < SAME_SESSION_PROMOTION_COUNT) {
                promotionsToSave.add(
                        PromotionRecord.builder()
                                .schoolId(student.getSchoolId())
                                .studentId(student.getId())
                                .sourceSessionId(sessions.active().getId())
                                .targetSessionId(sessions.active().getId())
                                .sourceClassId(plan.currentClass().getId())
                                .targetClassId(plan.currentClass().getId())
                                .promotionType(PromotionType.PROMOTE)
                                .promotedBy("SYSTEM_SEEDER")
                                .promotedAt(LocalDateTime.of(2024, 8, 15, 10, 0).plusMinutes(sameSessionPromotionCounter))
                                .remarks("Same-session section transfer")
                                .build()
                );
                sameSessionPromotionCounter++;
            }
        }

        BatchSaveUtil.saveInBatches(enrollmentsToSave, 1_000, studentEnrollmentRepository::saveAll);
        BatchSaveUtil.saveInBatches(promotionsToSave, 1_000, promotionRecordRepository::saveAll);

        Map<Long, List<Student>> studentsBySchool = savedStudents.stream()
                .collect(Collectors.groupingBy(Student::getSchoolId, LinkedHashMap::new, Collectors.toList()));

        Map<Long, List<StudentEnrollment>> enrollmentsBySchool = enrollmentsToSave.stream()
                .collect(Collectors.groupingBy(StudentEnrollment::getSchoolId, LinkedHashMap::new, Collectors.toList()));

        Map<Long, Map<Long, List<StudentEnrollment>>> enrollmentsBySchoolAndSession = new LinkedHashMap<>();
        for (Map.Entry<Long, List<StudentEnrollment>> entry : enrollmentsBySchool.entrySet()) {
            Map<Long, List<StudentEnrollment>> bySession = entry.getValue().stream()
                    .collect(Collectors.groupingBy(StudentEnrollment::getSessionId, LinkedHashMap::new, Collectors.toList()));
            enrollmentsBySchoolAndSession.put(entry.getKey(), bySession);
        }

        Map<Long, Student> studentsById = savedStudents.stream()
                .collect(Collectors.toMap(Student::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));

        for (List<Student> students : studentsBySchool.values()) {
            students.sort(Comparator.comparing(Student::getId));
        }

        return new Result(studentsBySchool, studentsById, enrollmentsBySchoolAndSession, enrollmentsToSave);
    }

    private StudentEnrollment buildEnrollment(
            Student student,
            Long classId,
            Long sessionId,
            AdmissionType admissionType,
            Map<Long, Integer> rollByClassSession
    ) {
        Long key = (classId * 100_000L) + sessionId;
        int roll = rollByClassSession.getOrDefault(key, 0) + 1;
        rollByClassSession.put(key, roll);

        return StudentEnrollment.builder()
                .schoolId(student.getSchoolId())
                .studentId(student.getId())
                .classId(classId)
                .section("A")
                .sessionId(sessionId)
                .rollNumber(roll)
                .enrollmentDate(LocalDate.of(2024, 4, 1))
                .startDate(LocalDate.of(2024, 4, 1))
                .admissionType(admissionType)
                .active(true)
                .remarks("Seeded enrollment")
                .build();
    }

    private StudentType typeForIndex(int index) {
        if (index < CURRENT_ONLY_COUNT) {
            return StudentType.CURRENT_ONLY;
        }
        if (index < CURRENT_ONLY_COUNT + PAST_ONLY_COUNT) {
            return StudentType.PAST_ONLY;
        }
        return StudentType.PROMOTED;
    }

    private String nextClassName(String className) {
        int idx = CLASS_FLOW.indexOf(className);
        if (idx < 0 || idx == CLASS_FLOW.size() - 1) {
            return className;
        }
        return CLASS_FLOW.get(idx + 1);
    }

    private enum StudentType {
        CURRENT_ONLY,
        PAST_ONLY,
        PROMOTED
    }

    private record StudentPlan(
            Student student,
            StudentType type,
            SchoolClass pastClass,
            SchoolClass currentClass,
            SessionSeeder.SessionTriplet sessions
    ) {
    }

    public record Result(
            Map<Long, List<Student>> studentsBySchool,
            Map<Long, Student> studentsById,
            Map<Long, Map<Long, List<StudentEnrollment>>> enrollmentsBySchoolAndSession,
            List<StudentEnrollment> allEnrollments
    ) {
    }
}
