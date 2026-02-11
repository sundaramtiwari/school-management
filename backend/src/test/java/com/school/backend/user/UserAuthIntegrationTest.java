package com.school.backend.user;

import com.school.backend.common.TestAuthHelper;
import com.school.backend.common.enums.UserRole;
import com.school.backend.core.student.dto.SchoolCreateRequest;
import com.school.backend.school.dto.SchoolDto;
import com.school.backend.school.repository.SchoolRepository;
import com.school.backend.user.dto.AuthResponse;
import com.school.backend.user.dto.LoginRequest;
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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class UserAuthIntegrationTest {

        @Autowired
        private TestRestTemplate restTemplate;

        @Autowired
        private TestAuthHelper authHelper;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private SchoolRepository schoolRepository;

        @Autowired
        private PasswordEncoder passwordEncoder;

        private String token;
        private HttpHeaders headers;

        private Long userId;
        private Long schoolId;

        // ------------------------------------------------

        @BeforeEach
        void setup() {

                token = authHelper.createSuperAdminAndLogin();
                headers = authHelper.authHeaders(token);
        }

        // ------------------------------------------------

        @Test
        void shouldLoginSuccessfully() {

                // 1. Create school (AUTHENTICATED)

                schoolId = createSchool("Login Test School");

                // 2. Create user

                User user = User.builder()
                                .email("login@test.com")
                                .passwordHash(
                                                passwordEncoder.encode("secret123"))
                                .role(UserRole.SCHOOL_ADMIN)
                                .school(
                                                schoolRepository.findById(schoolId)
                                                                .orElseThrow())
                                .active(true)
                                .build();

                user = userRepository.save(user);

                userId = user.getId();

                // 3. Login (PUBLIC API)

                LoginRequest req = new LoginRequest();

                req.setEmail("login@test.com");
                req.setPassword("secret123");

                ResponseEntity<AuthResponse> res = restTemplate.postForEntity(
                                "/api/auth/login",
                                req,
                                AuthResponse.class);

                // 4. Verify

                assertThat(res.getStatusCode())
                                .isEqualTo(HttpStatus.OK);

                AuthResponse body = res.getBody();

                assertThat(body).isNotNull();
                assertThat(body.getToken()).isNotBlank();

                assertThat(body.getRole())
                                .isEqualTo(UserRole.SCHOOL_ADMIN);

                assertThat(body.getSchoolId())
                                .isEqualTo(schoolId);

                assertThat(body.getUserId())
                                .isEqualTo(userId);
        }

        // ------------------------------------------------

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

                return res.getBody().getId();
        }

        // ------------------------------------------------

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
