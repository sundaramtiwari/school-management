package com.school.backend.common;

import com.school.backend.common.enums.UserRole;
import com.school.backend.user.dto.AuthResponse;
import com.school.backend.user.dto.LoginRequest;
import com.school.backend.user.entity.User;
import com.school.backend.user.repository.UserRepository;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class TestAuthHelper {

    private final TestRestTemplate restTemplate;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public TestAuthHelper(TestRestTemplate restTemplate,
                          UserRepository userRepository,
                          PasswordEncoder passwordEncoder) {

        this.restTemplate = restTemplate;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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
                    .school(null) // will be set later if needed
                    .build();

            userRepository.save(user);
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
