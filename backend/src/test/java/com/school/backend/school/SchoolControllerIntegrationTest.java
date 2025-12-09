package com.school.backend.school;

import com.school.backend.common.dto.PageResponse;
import com.school.backend.school.dto.SchoolDto;
import com.school.backend.school.repository.SchoolRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD) // isolate tests
public class SchoolControllerIntegrationTest {

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private SchoolRepository schoolRepository;

    private static final String BASE = "/api/schools";

    @BeforeEach
    void clean() {
        schoolRepository.deleteAll();
    }

    private SchoolDto createSample(String name, String code, String city) {
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

        ResponseEntity<SchoolDto> resp = testRestTemplate.postForEntity(BASE, dto, SchoolDto.class);
        assertEquals(HttpStatus.CREATED, resp.getStatusCode(), "Expected 201 Created on POST");
        SchoolDto created = resp.getBody();
        assertNotNull(created);
        assertNotNull(created.getId(), "Created school must have id");
        return created;
    }

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

        // Use ParameterizedTypeReference for PageResponse<SchoolDto>
        ParameterizedTypeReference<PageResponse<SchoolDto>> ptr =
                new ParameterizedTypeReference<>() {
                };

        ResponseEntity<PageResponse<SchoolDto>> response = testRestTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                ptr
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        PageResponse<SchoolDto> page = response.getBody();
        assertNotNull(page, "PageResponse must not be null");

        // verify metadata
        assertEquals(0, page.number(), "page number should be 0");
        assertEquals(2, page.size(), "page size should be 2");
        assertTrue(page.totalElements() >= 5, "totalElements should be >= 5");

        // content checks
        List<SchoolDto> content = page.content();
        assertNotNull(content);
        assertEquals(2, content.size(), "expected 2 items on page 0");

        // check that returned items have required fields
        SchoolDto first = content.get(0);
        assertNotNull(first.getId());
        assertNotNull(first.getSchoolCode());
        assertNotNull(first.getName());

        // sanity: repository count matches totalElements
        long repoCount = schoolRepository.count();
        assertEquals(repoCount, page.totalElements(), "totalElements must match repository count");
    }
}
