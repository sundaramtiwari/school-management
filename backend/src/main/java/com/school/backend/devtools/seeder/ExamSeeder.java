package com.school.backend.devtools.seeder;

import com.school.backend.common.enums.ExamStatus;
import com.school.backend.core.classsubject.entity.ClassSubject;
import com.school.backend.core.classsubject.entity.SchoolClass;
import com.school.backend.core.student.entity.StudentEnrollment;
import com.school.backend.testmanagement.entity.Exam;
import com.school.backend.testmanagement.entity.ExamSubject;
import com.school.backend.testmanagement.entity.StudentMark;
import com.school.backend.testmanagement.repository.ExamRepository;
import com.school.backend.testmanagement.repository.ExamSubjectRepository;
import com.school.backend.testmanagement.repository.StudentMarkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ExamSeeder {
    private static final String[] EXAM_TYPES = {
            "UNIT_TEST_1",
            "HALF_YEARLY",
            "UNIT_TEST_2",
            "UNIT_TEST_3",
            "ANNUAL"
    };

    private final ExamRepository examRepository;
    private final ExamSubjectRepository examSubjectRepository;
    private final StudentMarkRepository studentMarkRepository;

    @Transactional
    public void seed(Random random, ClassSubjectSeeder.Result classSubjectResult, StudentSeeder.Result studentResult) {
        List<Exam> examsToSave = new ArrayList<>();
        for (List<SchoolClass> classes : classSubjectResult.classesBySchool().values()) {
            for (SchoolClass schoolClass : classes) {
                for (int i = 0; i < EXAM_TYPES.length; i++) {
                    String examType = EXAM_TYPES[i];
                    LocalDate startDate = examDateByIndex(i + 1, schoolClass.getSessionId());
                    examsToSave.add(
                            Exam.builder()
                                    .schoolId(schoolClass.getSchoolId())
                                    .classId(schoolClass.getId())
                                    .sessionId(schoolClass.getSessionId())
                                    .name(examType)
                                    .examType(examType)
                                    .startDate(startDate)
                                    .endDate(startDate.plusDays(4))
                                    .status(ExamStatus.PUBLISHED)
                                    .active(true)
                                    .build()
                    );
                }
            }
        }
        List<Exam> exams = examRepository.saveAll(examsToSave);

        Map<Long, List<ClassSubject>> classSubjectsByClass = classSubjectResult.classSubjectsByClassId();
        List<ExamSubject> examSubjectsToSave = new ArrayList<>();
        for (Exam exam : exams) {
            List<ClassSubject> classSubjects = classSubjectsByClass.getOrDefault(exam.getClassId(), List.of());
            for (ClassSubject classSubject : classSubjects) {
                examSubjectsToSave.add(
                        ExamSubject.builder()
                                .schoolId(exam.getSchoolId())
                                .examId(exam.getId())
                                .subjectId(classSubject.getSubject().getId())
                                .maxMarks(100)
                                .active(true)
                                .build()
                );
            }
        }
        List<ExamSubject> examSubjects = examSubjectRepository.saveAll(examSubjectsToSave);

        Map<Long, List<StudentEnrollment>> enrollmentsByClassSession = new LinkedHashMap<>();
        for (StudentEnrollment enrollment : studentResult.allEnrollments()) {
            Long key = composite(enrollment.getClassId(), enrollment.getSessionId());
            enrollmentsByClassSession.computeIfAbsent(key, k -> new ArrayList<>()).add(enrollment);
        }

        Map<Long, List<ExamSubject>> examSubjectsByExamId = examSubjects.stream()
                .collect(Collectors.groupingBy(ExamSubject::getExamId, LinkedHashMap::new, Collectors.toList()));

        List<StudentMark> marksToSave = new ArrayList<>();
        for (Exam exam : exams) {
            List<ExamSubject> subjects = examSubjectsByExamId.getOrDefault(exam.getId(), List.of());
            List<StudentEnrollment> enrollments = enrollmentsByClassSession.getOrDefault(
                    composite(exam.getClassId(), exam.getSessionId()),
                    List.of()
            );

            for (ExamSubject examSubject : subjects) {
                for (StudentEnrollment enrollment : enrollments) {
                    marksToSave.add(
                            StudentMark.builder()
                                    .schoolId(exam.getSchoolId())
                                    .examId(exam.getId())
                                    .examSubjectId(examSubject.getId())
                                    .studentId(enrollment.getStudentId())
                                    .marksObtained(generateMarks(random, enrollment.getStudentId(), exam.getId(), examSubject.getId()))
                                    .remarks("Seeded mark")
                                    .build()
                    );
                }
            }
        }

        BatchSaveUtil.saveInBatches(marksToSave, 2_000, studentMarkRepository::saveAll);
    }

    private LocalDate examDateByIndex(int examIndex, Long sessionId) {
        int yearBase = (int) (sessionId % 10) + 2023;
        return LocalDate.of(yearBase, Math.min(examIndex * 2, 10), 10);
    }

    private int generateMarks(Random random, Long studentId, Long examId, Long examSubjectId) {
        long signal = (studentId + examId + examSubjectId) % 10;
        if (signal == 0) {
            return 20 + random.nextInt(12);
        }
        return 45 + random.nextInt(51);
    }

    private Long composite(Long classId, Long sessionId) {
        return classId * 100_000L + sessionId;
    }
}
