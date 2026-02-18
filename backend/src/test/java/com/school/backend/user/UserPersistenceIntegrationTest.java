package com.school.backend.user;

import com.school.backend.common.TestAuthHelper;
import com.school.backend.common.enums.UserRole;
import com.school.backend.core.student.dto.SchoolCreateRequest;
import com.school.backend.school.dto.SchoolDto;
import com.school.backend.school.repository.SchoolRepository;
import com.school.backend.user.entity.User;
import com.school.backend.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class UserPersistenceIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SchoolRepository schoolRepository;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TestAuthHelper authHelper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String token;
    private HttpHeaders headers;

    private Long schoolId;
    private Long userId;

    // ----------------------------------------------------

    @BeforeEach
    void setup() {
        token = authHelper.createSuperAdminAndLogin();
        headers = authHelper.authHeaders(token);
    }

    // ----------------------------------------------------

    @Test
    void shouldCreateAndLoadUser() {

        // 1. Create School (AUTHENTICATED)

        schoolId = createSchool("Auth Test School");

        // 2. Create User

        User user = User.builder()
                .email("admin@test.com")
                .passwordHash(
                        passwordEncoder.encode("password123"))
                .role(UserRole.SCHOOL_ADMIN)
                .school(
                        schoolRepository
                                .findById(schoolId)
                                .orElseThrow())
                .active(true)
                .build();

        User saved = userRepository.save(user);

        userId = saved.getId();

        // 3. Verify Saved

        assertThat(saved.getId()).isNotNull();

        assertThat(saved.getEmail())
                .isEqualTo("admin@test.com");

        assertThat(saved.getRole())
                .isEqualTo(UserRole.SCHOOL_ADMIN);

        assertThat(saved.isActive())
                .isTrue();

        assertThat(saved.getSchool().getId())
                .isEqualTo(schoolId);

        // 4. Load From DB

        User loaded = userRepository.findById(userId)
                .orElseThrow();

        // 5. Verify Loaded

        assertThat(loaded.getEmail())
                .isEqualTo("admin@test.com");

        assertThat(
                passwordEncoder.matches(
                        "password123",
                        loaded.getPasswordHash()))
                .isTrue();

        assertThat(loaded.getRole())
                .isEqualTo(UserRole.SCHOOL_ADMIN);

        assertThat(loaded.getSchool().getId())
                .isEqualTo(schoolId);

        assertThat(loaded.isActive())
                .isTrue();
    }

    @Test
    void shouldCreateSuperAdminUserWithNullSchool() {
        User superAdmin = User.builder()
                .email("superadmin@test.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .role(UserRole.SUPER_ADMIN)
                .school(null)
                .active(true)
                .build();

        User saved = userRepository.save(superAdmin);
        assertThat(saved.getSchool()).isNull();
    }
    // ----------------------------------------------------

    private Long createSchool(String name) {

        SchoolCreateRequest req = new SchoolCreateRequest();

        req.setName(name);
        req.setDisplayName(name);
        req.setBoard("CBSE");
        req.setAddress("Varanasi");

        HttpEntity<SchoolCreateRequest> entity = new HttpEntity<>(req, headers);

        ResponseEntity<SchoolDto> res = restTemplate.exchange(
                "/api/schools",
                HttpMethod.POST,
                entity,
                SchoolDto.class);

        assertThat(res.getStatusCode())
                .isEqualTo(HttpStatus.CREATED);

        return Objects.requireNonNull(res.getBody()).getId();
    }

    // ----------------------------------------------------

    @AfterEach
    void cleanup() {

        if (userId != null) {
            userRepository.deleteById(userId);
        }

        if (schoolId != null) {
            schoolRepository.deleteById(schoolId);
        }
    }
}
