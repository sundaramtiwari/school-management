package com.school.backend.common;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class BaseAuthenticatedIntegrationTest {

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected TestAuthHelper authHelper;

    protected String token;
    protected HttpHeaders headers;

    // Default: super admin
    @BeforeEach
    void baseSetup() {
        loginAsSuperAdmin();
    }

    protected void loginAsSuperAdmin() {
        token = authHelper.createSuperAdminAndLogin();
        headers = authHelper.authHeaders(token);
    }

    protected void loginAsSchoolAdmin(Long schoolId) {
        token = authHelper.createSchoolAdminAndLogin(schoolId);
        headers = authHelper.authHeaders(token);
    }
}
