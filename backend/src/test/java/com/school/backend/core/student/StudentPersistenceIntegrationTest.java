package com.school.backend.core.student;

import com.school.backend.common.BaseAuthenticatedIntegrationTest;
import com.school.backend.common.enums.Gender;
import com.school.backend.core.guardian.dto.GuardianCreateRequest;
import com.school.backend.core.student.dto.StudentCreateRequest;
import com.school.backend.core.student.dto.StudentDto;
import com.school.backend.core.student.dto.StudentUpdateRequest;
import com.school.backend.school.entity.School;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class StudentPersistenceIntegrationTest extends BaseAuthenticatedIntegrationTest {

    @Test
    void shouldPersistAllStudentFields() {
        fullCleanup();
        School school = schoolRepository.save(School.builder()
                .name("Persistence Test School")
                .displayName("Persistence Test School")
                .schoolCode("TEST-PERSIST-001")
                .board("CBSE")
                .address("Varanasi")
                .active(true)
                .build());
        Long schoolId = school.getId();
        loginAsSchoolAdmin(schoolId);
        // Create Session & Set Header
        Long sessionId = setupSession(schoolId, sessionRepository, schoolRepository);
        setSessionHeader(sessionId);

        // Create dummy class (required for registration)
        schoolClassRepository.save(com.school.backend.core.classsubject.entity.SchoolClass.builder()
                .name("1")
                .schoolId(schoolId)
                .sessionId(sessionId)
                .active(true)
                .build());

        // 1. Register Student
        StudentCreateRequest createReq = new StudentCreateRequest();
        createReq.setAdmissionNumber("ST-FULL-001");
        createReq.setFirstName("John");
        createReq.setLastName("Doe");
        createReq.setDob(LocalDate.of(2010, 5, 15));
        createReq.setGender(Gender.MALE);
        createReq.setPen("PEN123456");
        createReq.setAadharNumber("123456789012");
        createReq.setReligion("Christianity");
        createReq.setCaste("General");
        createReq.setCategory("General");
        createReq.setAddress("123 Main St");
        createReq.setCity("New York");
        createReq.setState("NY");
        createReq.setPincode("10001");
        createReq.setContactNumber("1234567890");
        createReq.setEmail("john.doe@example.com");
        createReq.setBloodGroup("O+");
        createReq.setDateOfAdmission(LocalDate.now());
        createReq.setRemarks("Excellent student");
        createReq.setPreviousSchoolName("Old School");
        createReq.setPreviousSchoolBoard("CBSE");
        createReq.setPreviousClass("4th");
        createReq.setPreviousYearOfPassing(2024);
        createReq.setTransferCertificateNumber("TC123");
        createReq.setPreviousSchoolAddress("456 Old St");
        createReq.setPreviousSchoolContact("9876543210");
        createReq.setReasonForLeavingPreviousSchool("Relocation");
        createReq.setGuardians(List.of(GuardianCreateRequest.builder()
                .name("Persistence Guardian")
                .contactNumber("6655443322")
                .relation("FATHER")
                .primaryGuardian(true)
                .build()));

        HttpEntity<StudentCreateRequest> createEntity = new HttpEntity<>(createReq, headers);
        ResponseEntity<StudentDto> createRes = restTemplate.postForEntity("/api/students", createEntity,
                StudentDto.class);

        assertThat(createRes.getStatusCode().is2xxSuccessful()).isTrue();
        StudentDto saved = createRes.getBody();
        assertThat(saved).isNotNull();
        Long studentId = saved.getId();

        // 2. Verify all fields in created DTO
        assertStudentFields(saved, createReq);

        // 3. Update Student with several missing fields
        StudentUpdateRequest updateReq = new StudentUpdateRequest();
        updateReq.setFirstName("Johnny");
        updateReq.setAadharNumber("987654321012");
        updateReq.setAddress("789 New St");
        updateReq.setBloodGroup("A+");
        updateReq.setPreviousSchoolContact("0000000000");

        HttpEntity<StudentUpdateRequest> updateEntity = new HttpEntity<>(updateReq, headers);
        ResponseEntity<StudentDto> updateRes = restTemplate.exchange("/api/students/" + studentId,
                HttpMethod.PUT,
                updateEntity, StudentDto.class);

        assertThat(updateRes.getStatusCode().is2xxSuccessful()).isTrue();
        StudentDto updated = updateRes.getBody();
        assertThat(updated).isNotNull();

        // 4. Load from API to verify persistence
        ResponseEntity<StudentDto> getRes = restTemplate.exchange("/api/students/" + studentId, HttpMethod.GET,
                new HttpEntity<>(headers), StudentDto.class);
        StudentDto loaded = getRes.getBody();
        assertThat(loaded).isNotNull();

        assertThat(loaded.getFirstName()).isEqualTo("Johnny");
        assertThat(loaded.getAadharNumber()).isEqualTo("987654321012");
        assertThat(loaded.getAddress()).isEqualTo("789 New St");
        assertThat(loaded.getBloodGroup()).isEqualTo("A+");
        assertThat(loaded.getPreviousSchoolContact()).isEqualTo("0000000000");

        // Ensure other fields are still preserved
        assertThat(loaded.getLastName()).isEqualTo("Doe");
        assertThat(loaded.getPen()).isEqualTo("PEN123456");
        assertThat(loaded.getPreviousSchoolName()).isEqualTo("Old School");
    }

    private void assertStudentFields(StudentDto dto, StudentCreateRequest req) {
        assertThat(dto.getAdmissionNumber()).isEqualTo(req.getAdmissionNumber());
        assertThat(dto.getFirstName()).isEqualTo(req.getFirstName());
        assertThat(dto.getLastName()).isEqualTo(req.getLastName());
        assertThat(dto.getDob()).isEqualTo(req.getDob());
        assertThat(dto.getGender()).isEqualTo(req.getGender());
        assertThat(dto.getPen()).isEqualTo(req.getPen());
        assertThat(dto.getAadharNumber()).isEqualTo(req.getAadharNumber());
        assertThat(dto.getReligion()).isEqualTo(req.getReligion());
        assertThat(dto.getCaste()).isEqualTo(req.getCaste());
        assertThat(dto.getCategory()).isEqualTo(req.getCategory());
        assertThat(dto.getAddress()).isEqualTo(req.getAddress());
        assertThat(dto.getCity()).isEqualTo(req.getCity());
        assertThat(dto.getState()).isEqualTo(req.getState());
        assertThat(dto.getPincode()).isEqualTo(req.getPincode());
        assertThat(dto.getContactNumber()).isEqualTo(req.getContactNumber());
        assertThat(dto.getEmail()).isEqualTo(req.getEmail());
        assertThat(dto.getBloodGroup()).isEqualTo(req.getBloodGroup());
        assertThat(dto.getDateOfAdmission()).isEqualTo(req.getDateOfAdmission());
        assertThat(dto.getRemarks()).isEqualTo(req.getRemarks());
        assertThat(dto.getPreviousSchoolName()).isEqualTo(req.getPreviousSchoolName());
        assertThat(dto.getPreviousSchoolBoard()).isEqualTo(req.getPreviousSchoolBoard());
        assertThat(dto.getPreviousClass()).isEqualTo(req.getPreviousClass());
        assertThat(dto.getPreviousYearOfPassing()).isEqualTo(req.getPreviousYearOfPassing());
        assertThat(dto.getTransferCertificateNumber()).isEqualTo(req.getTransferCertificateNumber());
        assertThat(dto.getPreviousSchoolAddress()).isEqualTo(req.getPreviousSchoolAddress());
        assertThat(dto.getPreviousSchoolContact()).isEqualTo(req.getPreviousSchoolContact());
        assertThat(dto.getReasonForLeavingPreviousSchool()).isEqualTo(req.getReasonForLeavingPreviousSchool());
    }
}
