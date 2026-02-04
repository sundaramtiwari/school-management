package com.school.backend.common;

import com.school.backend.common.enums.UserRole;
import com.school.backend.school.entity.School;
import com.school.backend.school.repository.SchoolRepository;
import com.school.backend.user.dto.AuthResponse;
import com.school.backend.user.dto.LoginRequest;
import com.school.backend.user.entity.User;
import com.school.backend.user.repository.UserRepository;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class TestAuthHelper {

    private final TestRestTemplate restTemplate;
    private final UserRepository userRepository;
    private final SchoolRepository schoolRepository;
    private final PasswordEncoder passwordEncoder;

    public TestAuthHelper(TestRestTemplate restTemplate,
                          UserRepository userRepository,
                          SchoolRepository schoolRepository,
                          PasswordEncoder passwordEncoder) {

        this.restTemplate = restTemplate;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.schoolRepository = schoolRepository;
    }

    // ------------------------------------------------

    public String createSuperAdminAndLogin() {

        String email = "super@test.com";
        String password = "admin123";

        // 1. Check if already exists
        User user = userRepository
                .findByEmailAndActiveTrue(email)
                .orElse(null);

        // 2. Create only if missing
        if (user == null) {

            user = User.builder()
                    .email(email)
                    .passwordHash(
                            passwordEncoder.encode(password)
                    )
                    .role(UserRole.SUPER_ADMIN)
                    .active(true)
                    .build();

            userRepository.save(user);
        }

        // 3. Login
        return login(email, password);
    }


    // ------------------------------------------------

    public String createSchoolAdminAndLogin(Long schoolId) {

        String email = "admin" + schoolId + "@test.com";
        String password = "admin123";

        School school = schoolRepository
                .findById(schoolId)
                .orElseThrow();

        // 1. Check if already exists
        User user = userRepository
                .findByEmailAndActiveTrue(email)
                .orElse(null);

        // 2. Create only if missing
        if (user == null) {

            user = User.builder()
                    .email(email)
                    .passwordHash(
                            passwordEncoder.encode(password)
                    )
                    .role(UserRole.SCHOOL_ADMIN)
                    .active(true)
                    .school(school)   // ✅ MUST be set
                    .build();

            userRepository.save(user);

        } else {

            // ensure correct school (safety)
            if (user.getSchool() == null ||
                    !user.getSchool().getId().equals(schoolId)) {

                user.setSchool(school);
                userRepository.save(user);
            }
        }

        return login(user.getEmail(), password);
    }


    // ------------------------------------------------

    private String login(String email, String password) {

        LoginRequest req = new LoginRequest();
        req.setEmail(email);
        req.setPassword(password);

        ResponseEntity<AuthResponse> res =
                restTemplate.postForEntity(
                        "/api/auth/login",
                        req,
                        AuthResponse.class
                );

        if (res.getStatusCode() != HttpStatus.OK) {
            throw new RuntimeException("Test login failed");
        }

        return res.getBody().getToken();
    }

    // ------------------------------------------------

    public HttpHeaders authHeaders(String token) {

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        return headers;
    }
}
