package com.school.backend.devtools.seeder;

import com.school.backend.common.enums.UserRole;
import com.school.backend.core.teacher.entity.Teacher;
import com.school.backend.core.teacher.repository.TeacherRepository;
import com.school.backend.school.entity.School;
import com.school.backend.school.repository.SchoolRepository;
import com.school.backend.user.entity.User;
import com.school.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Component
@RequiredArgsConstructor
public class SchoolSeeder {

    private static final int SCHOOL_COUNT = 10;

    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final TeacherRepository teacherRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Result seed(Random random) {
        List<School> schoolsToSave = new ArrayList<>(SCHOOL_COUNT);
        for (int i = 1; i <= SCHOOL_COUNT; i++) {
            String code = String.format("SCH%03d", i);
            schoolsToSave.add(
                    School.builder()
                            .name("School " + i)
                            .displayName("School " + i + " Public Academy")
                            .board("CBSE")
                            .medium("English")
                            .schoolCode(code)
                            .address("Sector " + i + ", Education District")
                            .city("City " + i)
                            .state("State " + ((i % 5) + 1))
                            .pincode(String.format("40%04d", i))
                            .contactNumber(String.format("88%08d", i))
                            .contactEmail("admin@" + code.toLowerCase() + ".school")
                            .website("https://www." + code.toLowerCase() + ".school")
                            .description("Deterministic dev seeding school " + i)
                            .active(true)
                            .build()
            );
        }

        List<School> schools = schoolRepository.saveAll(schoolsToSave);

        List<User> usersToSave = new ArrayList<>(2 + schools.size() * 56);

        for (School school : schools) {
            usersToSave.add(
                    User.builder()
                            .email("schooladmin@" + school.getSchoolCode().toLowerCase() + ".local")
                            .fullName(school.getName() + " Admin")
                            .passwordHash(passwordEncoder.encode("admin"))
                            .role(UserRole.SCHOOL_ADMIN)
                            .school(school)
                            .active(true)
                            .build()
            );

            for (int i = 1; i <= 5; i++) {
                usersToSave.add(
                        User.builder()
                                .email("accountant" + i + "@" + school.getSchoolCode().toLowerCase() + ".local")
                                .fullName("Accountant " + i + " " + school.getName())
                                .passwordHash(passwordEncoder.encode("admin"))
                                .role(UserRole.ACCOUNTANT)
                                .school(school)
                                .active(true)
                                .build()
                );
            }

            for (int i = 1; i <= 50; i++) {
                usersToSave.add(
                        User.builder()
                                .email("teacher" + i + "@" + school.getSchoolCode().toLowerCase() + ".local")
                                .fullName("Teacher " + i + " " + school.getName())
                                .passwordHash(passwordEncoder.encode("admin"))
                                .role(UserRole.TEACHER)
                                .school(school)
                                .active(true)
                                .build()
                );
            }
        }

        List<User> users = userRepository.saveAll(usersToSave);

        List<Teacher> teachersToSave = new ArrayList<>(schools.size() * 50);
        for (User user : users) {
            if (user.getRole() == UserRole.TEACHER && user.getSchool() != null) {
                Teacher teacher = Teacher.builder().user(user).build();
                teacher.setSchoolId(user.getSchool().getId());
                teachersToSave.add(teacher);
            }
        }
        List<Teacher> teachers = teacherRepository.saveAll(teachersToSave);

        Map<Long, List<Teacher>> teachersBySchool = new LinkedHashMap<>();
        for (School school : schools) {
            teachersBySchool.put(school.getId(), new ArrayList<>());
        }
        for (Teacher teacher : teachers) {
            teachersBySchool.get(teacher.getSchoolId()).add(teacher);
        }

        return new Result(schools, teachersBySchool);
    }

    public record Result(
            List<School> schools,
            Map<Long, List<Teacher>> teachersBySchool
    ) {
    }
}
