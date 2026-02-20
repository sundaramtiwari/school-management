package com.school.backend.school;

import com.school.backend.common.BaseAuthenticatedIntegrationTest;
import com.school.backend.common.dto.PageResponse;
import com.school.backend.school.dto.SchoolDto;
import com.school.backend.school.dto.SchoolOnboardingRequest;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

public class SchoolControllerIntegrationTest extends BaseAuthenticatedIntegrationTest {

    private static final String BASE = "/api/schools";

    @BeforeEach
    void setup() {
        fullCleanup();
        loginAsSuperAdmin();
    }

    // ------------------------------------------------

    private SchoolDto createSample(String name,
                                   String code,
                                   String city) {

        SchoolDto dto = SchoolDto.builder()
                .name(name)
                .displayName(name)
                .board("CBSE")
                .medium("English")
                .schoolCode(code)
                .city(city)
                .state("UP")
                .pincode("221001")
                .contactNumber("9000000000")
                .contactEmail("info@" + code.toLowerCase() + ".edu")
                .website("https://" + code.toLowerCase() + ".edu")
                .active(true)
                .build();

        HttpEntity<SchoolDto> entity = new HttpEntity<>(dto, headers);

        ResponseEntity<SchoolDto> resp = restTemplate.exchange(
                BASE,
                HttpMethod.POST,
                entity,
                SchoolDto.class);

        assertEquals(
                HttpStatus.CREATED,
                resp.getStatusCode(),
                "Expected 201 Created on POST");

        SchoolDto created = Objects.requireNonNull(resp.getBody());

        assertNotNull(created);
        assertNotNull(
                created.getId(),
                "Created school must have id");

        return created;
    }

    // ------------------------------------------------

    @Test
    void fullPaginationFlow_createsAndPagesWithoutJacksonPageImplErrors() {

        // create 5 schools
        createSample("Sunrise Public School", "SPS001", "Varanasi");
        createSample("Riverdale High", "RHS002", "Lucknow");
        createSample("Green Valley School", "GVS003", "Allahabad");
        createSample("Horizon School", "HS004", "Varanasi");
        createSample("Maple Leaf Academy", "MLA005", "Varanasi");

        // build URI with page params
        String url = UriComponentsBuilder.fromPath(BASE)
                .queryParam("page", 0)
                .queryParam("size", 2)
                .build()
                .toUriString();

        ParameterizedTypeReference<PageResponse<SchoolDto>> ptr = new ParameterizedTypeReference<>() {
        };

        HttpEntity<Void> listEntity = new HttpEntity<>(headers);

        ResponseEntity<PageResponse<SchoolDto>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                listEntity,
                ptr);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        PageResponse<SchoolDto> page = Objects.requireNonNull(response.getBody());

        assertNotNull(page, "PageResponse must not be null");

        // verify metadata
        assertEquals(0, page.number(), "page number should be 0");
        assertEquals(2, page.size(), "page size should be 2");
        assertTrue(
                page.totalElements() >= 5,
                "totalElements should be >= 5");

        // content checks
        List<SchoolDto> content = page.content();

        assertNotNull(content);
        assertEquals(2, content.size(), "expected 2 items on page 0");

        // check required fields
        SchoolDto first = content.get(0);

        assertNotNull(first.getId());
        assertNotNull(first.getSchoolCode());
        assertNotNull(first.getName());

        // sanity: repo count
        long repoCount = schoolRepository.count();

        assertEquals(
                repoCount,
                page.totalElements(),
                "totalElements must match repository count");
    }

    @Test
    void testOnboardSchool_createsSchoolAndAdmin() {
        // Arrange
        HttpEntity<SchoolOnboardingRequest> entity = getSchoolOnboardingRequestHttpEntity();

        // Act
        ResponseEntity<SchoolDto> resp = restTemplate.postForEntity(BASE + "/onboard", entity, SchoolDto.class);

        // Assert
        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        SchoolDto resultBody = Objects.requireNonNull(resp.getBody());
        assertEquals("ONB100", resultBody.getSchoolCode());

        // Verify Admin User Creation? We might need UserRepository injected or check
        // login?
        // Let's rely on success for now, or we can inject UserRepository to verify.
    }

    private @NonNull HttpEntity<SchoolOnboardingRequest> getSchoolOnboardingRequestHttpEntity() {
        SchoolOnboardingRequest req = new SchoolOnboardingRequest();
        req.setName("Onboarded School");
        req.setSchoolCode("ONB100");
        req.setAdminName("Admin One");
        req.setAdminEmail("admin@onb100.com");
        req.setAdminPassword("password123");
        req.setCity("Varanasi");
        req.setState("Uttar Pradesh");
        req.setMedium("English");
        req.setBoard("CBSE");

        return new HttpEntity<>(req, headers);
    }

    @Test
    void testSchoolAdmin_cannotAccessGetAll() {
        // 1. Create a school (will be done by super admin)
        SchoolDto s = createSample("My Admin School", "MAS001", "Delhi");

        // 2. Login as School Admin for this school
        loginAsSchoolAdmin(s.getId());

        // 3. Call GET /api/schools
        ParameterizedTypeReference<PageResponse<SchoolDto>> ptr = new ParameterizedTypeReference<>() {
        };
        ResponseEntity<PageResponse<SchoolDto>> response = restTemplate.exchange(
                BASE,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                ptr);

        // 4. Verify forbidden access as SCHOOL_ADMIN is no longer allowed on GET /api/schools
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void testSchoolAdmin_canAccessOwnSchool_viaGetById() {
        SchoolDto s = createSample("My School", "MYS001", "Mumbai");
        loginAsSchoolAdmin(s.getId());

        ResponseEntity<SchoolDto> response = restTemplate.exchange(
                BASE + "/id/" + s.getId(),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                SchoolDto.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        SchoolDto body = Objects.requireNonNull(response.getBody());
        assertEquals(s.getId(), body.getId());
    }

    @Test
    void testSchoolAdmin_cannotAccessOtherSchool_viaGetById() {
        SchoolDto s1 = createSample("School One", "SCH001", "City1");
        SchoolDto s2 = createSample("School Two", "SCH002", "City2");

        loginAsSchoolAdmin(s1.getId());

        ResponseEntity<String> response = restTemplate.exchange(
                BASE + "/id/" + s2.getId(),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);

        // Expect 403 Forbidden (or 500 if AccessDeniedException is not handled
        // globally, but usually 403)
        // Spring Boot default error mapping for AccessDeniedException is 403
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void testSchoolAdmin_canAccessOwnSchool_viaGetByCode() {
        SchoolDto s = createSample("My Code School", "MCS001", "Pune");
        loginAsSchoolAdmin(s.getId());

        ResponseEntity<SchoolDto> response = restTemplate.exchange(
                BASE + "/" + s.getSchoolCode(),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                SchoolDto.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        SchoolDto body = Objects.requireNonNull(response.getBody());
        assertEquals(s.getSchoolCode(), body.getSchoolCode());
    }

    @Test
    void testSchoolAdmin_cannotAccessOtherSchool_viaGetByCode() {
        SchoolDto s1 = createSample("Code School One", "CS100", "CityA");
        SchoolDto s2 = createSample("Code School Two", "CS200", "CityB");

        loginAsSchoolAdmin(s1.getId());

        ResponseEntity<String> response = restTemplate.exchange(
                BASE + "/" + s2.getSchoolCode(),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }
}
