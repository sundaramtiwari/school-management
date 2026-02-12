package com.school.backend.user;

import com.school.backend.common.BaseAuthenticatedIntegrationTest;
import com.school.backend.common.dto.PageResponse;
import com.school.backend.common.enums.UserRole;
import com.school.backend.school.entity.School;
import com.school.backend.user.dto.UserDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

public class UserControllerIntegrationTest extends BaseAuthenticatedIntegrationTest {

        private School schoolA;
        private School schoolB;

        @BeforeEach
        void setupTenants() {
                fullCleanup();

                schoolA = createSchool("School A", "SCH-A");
                schoolB = createSchool("School B", "SCH-B");
        }

        private School createSchool(String name, String code) {
                return schoolRepository.save(School.builder()
                                .name(name)
                                .schoolCode(code)
                                .active(true)
                                .build());
        }

        @Test
        void testTenantIsolation_schoolAdminCanOnlySeeOwnUsers() {
                // 1. Login as School A Admin
                loginAsSchoolAdmin(schoolA.getId());

                // 2. Create User in School A
                UserDto reqA = new UserDto();
                reqA.setEmail("teacher@schoola.com");
                reqA.setPassword("pass123");
                reqA.setRole(UserRole.TEACHER);
                reqA.setFullName("Teacher A");

                ResponseEntity<UserDto> respA = restTemplate.postForEntity(
                                "/api/users",
                                new HttpEntity<>(reqA, headers),
                                UserDto.class);
                assertEquals(HttpStatus.CREATED, respA.getStatusCode());

                // 3. Login as School B Admin
                loginAsSchoolAdmin(schoolB.getId());

                // 4. Try to List Users -> Should NOT see Teacher A
                ResponseEntity<PageResponse<UserDto>> listResp = restTemplate.exchange(
                                "/api/users",
                                HttpMethod.GET,
                                new HttpEntity<>(headers),
                                new ParameterizedTypeReference<>() {
                                });

                assertEquals(HttpStatus.OK, listResp.getStatusCode());
                PageResponse<UserDto> page = Objects.requireNonNull(listResp.getBody());
                assertNotNull(page);

                // Should verify content does not contain "teacher@schoola.com"
                boolean hasTeacherA = page.content().stream()
                                .anyMatch(u -> u.getEmail().equals("teacher@schoola.com"));

                assertFalse(hasTeacherA, "School B should not see School A's users");

                // 5. Create User in School B
                UserDto reqB = new UserDto();
                reqB.setEmail("teacher@schoolb.com");
                reqB.setPassword("pass123");
                reqB.setRole(UserRole.TEACHER);
                reqB.setFullName("Teacher B");

                restTemplate.postForEntity("/api/users", new HttpEntity<>(reqB, headers), UserDto.class);

                // 6. Verify List again -> Should see Teacher B
                ResponseEntity<PageResponse<UserDto>> listRespB = restTemplate.exchange(
                                "/api/users",
                                HttpMethod.GET,
                                new HttpEntity<>(headers),
                                new ParameterizedTypeReference<>() {
                                });
                assertTrue(Objects.requireNonNull(listRespB.getBody()).content().stream()
                                .anyMatch(u -> u.getEmail().equals("teacher@schoolb.com")));
        }
}
