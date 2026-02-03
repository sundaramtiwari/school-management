package com.school.backend.school;

import com.school.backend.common.TestAuthHelper;
import com.school.backend.common.dto.PageResponse;
import com.school.backend.school.dto.SchoolDto;
import com.school.backend.school.repository.SchoolRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class SchoolControllerIntegrationTest {

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private SchoolRepository schoolRepository;

    @Autowired
    private TestAuthHelper authHelper;

    private String token;
    private HttpHeaders headers;

    private static final String BASE = "/api/schools";

    // ------------------------------------------------

    @BeforeEach
    void setup() {

        token = authHelper.createSuperAdminAndLogin();
        headers = authHelper.authHeaders(token);

        schoolRepository.deleteAll();
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

        HttpEntity<SchoolDto> entity =
                new HttpEntity<>(dto, headers);

        ResponseEntity<SchoolDto> resp =
                testRestTemplate.exchange(
                        BASE,
                        HttpMethod.POST,
                        entity,
                        SchoolDto.class
                );

        assertEquals(
                HttpStatus.CREATED,
                resp.getStatusCode(),
                "Expected 201 Created on POST"
        );

        SchoolDto created = resp.getBody();

        assertNotNull(created);
        assertNotNull(
                created.getId(),
                "Created school must have id"
        );

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


        ParameterizedTypeReference<PageResponse<SchoolDto>> ptr =
                new ParameterizedTypeReference<>() {
                };


        HttpEntity<Void> listEntity =
                new HttpEntity<>(headers);

        ResponseEntity<PageResponse<SchoolDto>> response =
                testRestTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        listEntity,
                        ptr
                );


        assertEquals(HttpStatus.OK, response.getStatusCode());

        PageResponse<SchoolDto> page = response.getBody();

        assertNotNull(page, "PageResponse must not be null");


        // verify metadata
        assertEquals(0, page.number(), "page number should be 0");
        assertEquals(2, page.size(), "page size should be 2");
        assertTrue(
                page.totalElements() >= 5,
                "totalElements should be >= 5"
        );


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
                "totalElements must match repository count"
        );
    }
}
