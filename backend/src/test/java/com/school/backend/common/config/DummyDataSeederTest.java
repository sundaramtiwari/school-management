package com.school.backend.common.config;

import com.school.backend.core.student.repository.StudentRepository;
import com.school.backend.school.repository.SchoolRepository;
import com.school.backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
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

    @Test
    @Transactional
    void testSeederPopulatesData() throws Exception {
        // Arrange: Ensure DB is empty or has < 2 schools (H2 test db should be empty)
        long initialSchoolCount = schoolRepository.count();

        // Act
        dummyDataSeeder.run();

        // Assert
        long schoolCount = schoolRepository.count();
        long userCount = userRepository.count();
        long studentCount = studentRepository.count();

        if (initialSchoolCount > 1) {
            // Seeder returns early
            assertThat(schoolCount).isEqualTo(initialSchoolCount);
        } else {
            // Seeder should have run
            assertThat(schoolCount).isGreaterThanOrEqualTo(5);
            assertThat(userCount).isGreaterThan(5); // At least 5 admins + teachers
            assertThat(studentCount).isGreaterThan(100);
        }
    }
}
