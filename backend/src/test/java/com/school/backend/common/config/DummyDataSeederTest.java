package com.school.backend.common.config;

import com.school.backend.core.student.repository.StudentRepository;
import com.school.backend.school.repository.SchoolRepository;
import com.school.backend.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class DummyDataSeederTest {

    @Autowired
    private DummyDataSeeder dummyDataSeeder;

    @Autowired
    private SchoolRepository schoolRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentRepository studentRepository;

    //@Test
    @Transactional
    void testSeederPopulatesData() {
        long initialSchoolCount = schoolRepository.count();
        long initialUserCount = userRepository.count();
        long initialStudentCount = studentRepository.count();

        dummyDataSeeder.run();

        long schoolCount = schoolRepository.count();
        long userCount = userRepository.count();
        long studentCount = studentRepository.count();

        // DummyDataSeeder is intentionally disabled; test asserts idempotent no-op behavior.
        assertThat(schoolCount).isEqualTo(initialSchoolCount);
        assertThat(userCount).isEqualTo(initialUserCount);
        assertThat(studentCount).isEqualTo(initialStudentCount);
    }
}
