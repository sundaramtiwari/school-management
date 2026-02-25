package com.school.backend.devtools.seeder;

import com.school.backend.core.classsubject.entity.ClassSubject;
import com.school.backend.core.classsubject.entity.SchoolClass;
import com.school.backend.core.classsubject.entity.Subject;
import com.school.backend.core.classsubject.repository.ClassSubjectRepository;
import com.school.backend.core.classsubject.repository.SchoolClassRepository;
import com.school.backend.core.classsubject.repository.SubjectRepository;
import com.school.backend.core.teacher.entity.Teacher;
import com.school.backend.school.entity.AcademicSession;
import com.school.backend.school.entity.School;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ClassSubjectSeeder {

    private static final List<String> CLASS_NAMES = List.of(
            "Nursery", "LKG", "UKG", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12");

    private static final List<String> PRIMARY_SUBJECTS = List.of("English", "Hindi", "Maths", "EVS");
    private static final List<String> MIDDLE_SUBJECTS = List.of("English", "Hindi", "Maths", "Science", "SST",
            "Computer");
    private static final List<String> SECONDARY_SUBJECTS = List.of("English", "Hindi", "Maths", "Physics", "Chemistry",
            "Biology", "SST", "Computer");
    private static final List<String> ALL_SUBJECTS = List.of(
            "English", "Hindi", "Maths", "EVS", "Science", "SST", "Computer", "Physics", "Chemistry", "Biology");

    private final SchoolClassRepository schoolClassRepository;
    private final SubjectRepository subjectRepository;
    private final ClassSubjectRepository classSubjectRepository;

    @Transactional
    public Result seed(Random random, SchoolSeeder.Result schoolResult, SessionSeeder.Result sessionResult) {
        List<Subject> subjectsToSave = new ArrayList<>(schoolResult.schools().size() * ALL_SUBJECTS.size());
        for (School school : schoolResult.schools()) {
            for (String name : ALL_SUBJECTS) {
                Subject subject = Subject.builder()
                        .name(name)
                        .code(name.substring(0, Math.min(4, name.length())).toUpperCase())
                        .type("THEORY")
                        .maxMarks(100)
                        .minMarks(33)
                        .active(true)
                        .build();
                subject.setSchoolId(school.getId());
                subjectsToSave.add(subject);
            }
        }
        List<Subject> subjects = subjectRepository.saveAll(subjectsToSave);

        Map<Long, Map<String, Subject>> subjectsBySchoolAndName = new LinkedHashMap<>();
        for (Subject subject : subjects) {
            subjectsBySchoolAndName
                    .computeIfAbsent(subject.getSchoolId(), k -> new LinkedHashMap<>())
                    .put(subject.getName(), subject);
        }

        List<SchoolClass> classesToSave = new ArrayList<>();
        for (School school : schoolResult.schools()) {
            List<Teacher> teachers = schoolResult.teachersBySchool().get(school.getId());
            List<AcademicSession> sessions = List.of(
                    sessionResult.sessionsBySchool().get(school.getId()).completed(),
                    sessionResult.sessionsBySchool().get(school.getId()).active(),
                    sessionResult.sessionsBySchool().get(school.getId()).planned());

            int classIndex = 0;
            for (AcademicSession session : sessions) {
                for (String className : CLASS_NAMES) {
                    Teacher classTeacher = teachers.get((classIndex++) % teachers.size());
                    SchoolClass schoolClass = SchoolClass.builder()
                            .schoolId(school.getId())
                            .sessionId(session.getId())
                            .name(className)
                            .section("A")
                            .capacity(60)
                            .classTeacher(classTeacher)
                            .active(true)
                            .remarks("Seeded class")
                            .build();
                    classesToSave.add(schoolClass);
                }
            }
        }
        List<SchoolClass> classes = schoolClassRepository.saveAll(classesToSave);

        Map<Long, Map<Long, Map<String, SchoolClass>>> classBySchoolSessionName = new LinkedHashMap<>();
        Map<Long, List<SchoolClass>> classesBySchool = new LinkedHashMap<>();
        for (SchoolClass schoolClass : classes) {
            classBySchoolSessionName
                    .computeIfAbsent(schoolClass.getSchoolId(), k -> new LinkedHashMap<>())
                    .computeIfAbsent(schoolClass.getSessionId(), k -> new LinkedHashMap<>())
                    .put(schoolClass.getName(), schoolClass);
            classesBySchool.computeIfAbsent(schoolClass.getSchoolId(), k -> new ArrayList<>()).add(schoolClass);
        }

        List<ClassSubject> classSubjectsToSave = new ArrayList<>();
        for (School school : schoolResult.schools()) {
            List<Teacher> teachers = schoolResult.teachersBySchool().get(school.getId());
            List<SchoolClass> schoolClasses = classesBySchool.getOrDefault(school.getId(), List.of());
            int classCounter = 0;
            for (SchoolClass schoolClass : schoolClasses) {
                List<String> subjectNames = subjectsForClass(schoolClass.getName());
                for (int i = 0; i < subjectNames.size(); i++) {
                    Subject subject = subjectsBySchoolAndName.get(school.getId()).get(subjectNames.get(i));
                    Teacher teacher = teachers.get((classCounter + i) % teachers.size());
                    ClassSubject classSubject = ClassSubject.builder()
                            .schoolClass(schoolClass)
                            .subject(subject)
                            .teacher(teacher)
                            .displayOrder(i + 1)
                            .build();
                    classSubject.setSchoolId(school.getId());
                    classSubjectsToSave.add(classSubject);
                }
                classCounter++;
            }
        }

        List<ClassSubject> classSubjects = classSubjectRepository.saveAll(classSubjectsToSave);
        Map<Long, List<ClassSubject>> classSubjectsByClassId = classSubjects.stream()
                .collect(Collectors.groupingBy(cs -> cs.getSchoolClass().getId(), LinkedHashMap::new,
                        Collectors.toList()));

        return new Result(classesBySchool, classBySchoolSessionName, classSubjectsByClassId);
    }

    private List<String> subjectsForClass(String className) {
        if (isSecondary(className)) {
            return SECONDARY_SUBJECTS;
        }
        if (isMiddle(className)) {
            return MIDDLE_SUBJECTS;
        }
        return PRIMARY_SUBJECTS;
    }

    private boolean isMiddle(String className) {
        return "6".equals(className) || "7".equals(className) || "8".equals(className);
    }

    private boolean isSecondary(String className) {
        return "9".equals(className) || "10".equals(className) || "11".equals(className) || "12".equals(className);
    }

    public record Result(
            Map<Long, List<SchoolClass>> classesBySchool,
            Map<Long, Map<Long, Map<String, SchoolClass>>> classBySchoolSessionName,
            Map<Long, List<ClassSubject>> classSubjectsByClassId) {
    }
}
