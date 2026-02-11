package com.school.backend.core.student;

import com.school.backend.common.BaseAuthenticatedIntegrationTest;
import com.school.backend.common.dto.PageResponse;
import com.school.backend.common.enums.Gender;
import com.school.backend.core.classsubject.repository.SchoolClassRepository;
import com.school.backend.core.student.dto.*;
import com.school.backend.core.student.repository.PromotionRecordRepository;
import com.school.backend.core.student.repository.StudentEnrollmentRepository;
import com.school.backend.core.student.repository.StudentRepository;
import com.school.backend.fee.dto.*;
import com.school.backend.fee.entity.FeeType;
import com.school.backend.fee.repository.FeePaymentRepository;
import com.school.backend.fee.repository.FeeStructureRepository;
import com.school.backend.fee.repository.FeeTypeRepository;
import com.school.backend.fee.repository.StudentFeeAssignmentRepository;
import com.school.backend.school.entity.School;
import com.school.backend.school.repository.SchoolRepository;
import com.school.backend.user.repository.UserRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.Objects;

public class StudentFlowIntegrationTest extends BaseAuthenticatedIntegrationTest {

        @Autowired
        private FeePaymentRepository feePaymentRepository;
        @Autowired
        private FeeStructureRepository feeStructureRepository;
        @Autowired
        private FeeTypeRepository feeTypeRepository;
        @Autowired
        private StudentFeeAssignmentRepository assignmentRepository;
        @Autowired
        private PromotionRecordRepository promotionRecordRepository;
        @Autowired
        private StudentEnrollmentRepository studentEnrollmentRepository;
        @Autowired
        private StudentRepository studentRepository;
        @Autowired
        private SchoolClassRepository schoolClassRepository;
        @Autowired
        private SchoolRepository schoolRepository;
        @Autowired
        private UserRepository userRepository;

        private Long schoolId;
        private Long classId;
        private Long studentId;
        private Long feeStructureId;
        private Long feeTypeId;

        @Test
        void student_register_enroll_promote_flow() {

                // ---------- Create School ----------

                Map<String, Object> schoolReq = Map.of(
                                "name", "Student Module School",
                                "displayName", "SMS",
                                "board", "CBSE",
                                "schoolCode", "SMS-2025",
                                "contactEmail", "sms@example.com",
                                "city", "Varanasi",
                                "state", "Uttar Pradesh");

                HttpEntity<Map<String, Object>> schoolEntity = new HttpEntity<>(schoolReq, headers);

                ResponseEntity<School> schoolResp = restTemplate.exchange(
                                "/api/schools",
                                HttpMethod.POST,
                                schoolEntity,
                                School.class);

                Assertions.assertThat(schoolResp.getStatusCode())
                                .isEqualTo(HttpStatus.CREATED);

                schoolId = Objects.requireNonNull(schoolResp.getBody()).getId();

                // LOGIN AS SCHOOL ADMIN NOW
                loginAsSchoolAdmin(schoolId);

                // ---------- Create Class ----------

                Map<String, Object> classReq = Map.of("name", "1",
                                "session", "2025-26",
                                "schoolId", schoolId);

                HttpEntity<Map<String, Object>> classEntity = new HttpEntity<>(classReq, headers);

                ResponseEntity<Map<String, Object>> classResp = restTemplate.exchange(
                                "/api/classes",
                                HttpMethod.POST,
                                classEntity,
                                new ParameterizedTypeReference<Map<String, Object>>() {
                                });

                Assertions.assertThat(classResp.getStatusCode())
                                .isEqualTo(HttpStatus.OK);

                classId = Long.valueOf(
                                String.valueOf(Objects.requireNonNull(classResp.getBody()).get("id")));

                // ---------- Register Student ----------

                StudentCreateRequest sreq = new StudentCreateRequest();

                sreq.setAdmissionNumber("ADM-100");
                sreq.setFirstName("Test");
                sreq.setGender(Gender.MALE);

                HttpEntity<StudentCreateRequest> studentEntity = new HttpEntity<>(sreq, headers);

                ResponseEntity<StudentDto> sResp = restTemplate.exchange(
                                "/api/students",
                                HttpMethod.POST,
                                studentEntity,
                                StudentDto.class);

                Assertions.assertThat(sResp.getStatusCode())
                                .isEqualTo(HttpStatus.OK);

                studentId = Objects.requireNonNull(sResp.getBody()).getId();

                // ---------- Enroll Student ----------

                StudentEnrollmentRequest er = new StudentEnrollmentRequest();

                er.setStudentId(studentId);
                er.setClassId(classId);
                er.setSession("2025-26");

                HttpEntity<StudentEnrollmentRequest> enrollEntity = new HttpEntity<>(er, headers);

                ResponseEntity<StudentEnrollmentDto> enrollResp = restTemplate.exchange(
                                "/api/enrollments",
                                HttpMethod.POST,
                                enrollEntity,
                                StudentEnrollmentDto.class);

                Assertions.assertThat(enrollResp.getStatusCode())
                                .isEqualTo(HttpStatus.OK);

                // ---------- Fee Setup ----------

                FeeType type = new FeeType();
                type.setName("TUITION");

                HttpEntity<FeeType> typeEntity = new HttpEntity<>(type, headers);

                ResponseEntity<FeeType> typeResp = restTemplate.exchange(
                                "/api/fees/types",
                                HttpMethod.POST,
                                typeEntity,
                                FeeType.class);

                feeTypeId = Objects.requireNonNull(typeResp.getBody()).getId();

                FeeStructureCreateRequest fsReq = new FeeStructureCreateRequest();

                fsReq.setClassId(classId);
                fsReq.setSession("2025-26");
                fsReq.setFeeTypeId(feeTypeId);
                fsReq.setAmount(10000);

                HttpEntity<FeeStructureCreateRequest> fsEntity = new HttpEntity<>(fsReq, headers);

                ResponseEntity<FeeStructureDto> fsResp = restTemplate.exchange(
                                "/api/fees/structures",
                                HttpMethod.POST,
                                fsEntity,
                                FeeStructureDto.class);

                feeStructureId = Objects.requireNonNull(fsResp.getBody()).getId();

                StudentFeeAssignRequest assignReq = new StudentFeeAssignRequest();

                assignReq.setStudentId(studentId);
                assignReq.setFeeStructureId(feeStructureId);
                assignReq.setSession("2025-26");

                HttpEntity<StudentFeeAssignRequest> assignEntity = new HttpEntity<>(assignReq, headers);

                restTemplate.exchange(
                                "/api/fees/assignments",
                                HttpMethod.POST,
                                assignEntity,
                                StudentFeeAssignmentDto.class);

                FeePaymentRequest payReq = new FeePaymentRequest();

                payReq.setStudentId(studentId);
                payReq.setAmountPaid(10000);
                payReq.setMode("CASH");

                HttpEntity<FeePaymentRequest> payEntity = new HttpEntity<>(payReq, headers);

                restTemplate.exchange(
                                "/api/fees/payments",
                                HttpMethod.POST,
                                payEntity,
                                FeePaymentDto.class);

                // ---------- Promote ----------

                PromotionRequest pr = new PromotionRequest();

                pr.setToClassId(classId);
                pr.setToSection("B");
                pr.setSession("2025-26");
                pr.setPromoted(true);
                pr.setFeePending(false);
                pr.setRemarks("Promoted successfully");

                HttpEntity<PromotionRequest> promoteEntity = new HttpEntity<>(pr, headers);

                ResponseEntity<PromotionRecordDto> promoteRes = restTemplate.exchange(
                                "/api/students/" + studentId + "/history/promote",
                                HttpMethod.POST,
                                promoteEntity,
                                PromotionRecordDto.class);

                Assertions.assertThat(promoteRes.getStatusCode())
                                .isEqualTo(HttpStatus.OK);

                // ---------- List Enrollments ----------

                HttpEntity<Void> listEntity = new HttpEntity<>(headers);

                ResponseEntity<PageResponse<StudentEnrollmentDto>> pageResp = restTemplate.exchange(
                                "/api/enrollments/by-class/" + classId + "?page=0&size=10",
                                HttpMethod.GET,
                                listEntity,
                                new ParameterizedTypeReference<>() {
                                });

                Assertions.assertThat(pageResp.getStatusCode())
                                .isEqualTo(HttpStatus.OK);

                Assertions.assertThat(Objects.requireNonNull(pageResp.getBody()).content())
                                .isNotEmpty();
        }

        // ------------------------------------------------

        @Test
        void student_update_profile_flow() {

                // ---------- Create School ----------

                Map<String, Object> schoolReq = Map.of(
                                "name", "Update Test School",
                                "displayName", "UTS",
                                "board", "CBSE",
                                "schoolCode", "UTS-2025",
                                "contactEmail", "uts@example.com",
                                "city", "Varanasi",
                                "state", "Uttar Pradesh");

                HttpEntity<Map<String, Object>> schoolEntity = new HttpEntity<>(schoolReq, headers);

                ResponseEntity<School> schoolResp = restTemplate.exchange(
                                "/api/schools",
                                HttpMethod.POST,
                                schoolEntity,
                                School.class);

                Assertions.assertThat(schoolResp.getStatusCode())
                                .isEqualTo(HttpStatus.CREATED);

                schoolId = Objects.requireNonNull(schoolResp.getBody()).getId();

                loginAsSchoolAdmin(schoolId);
                // ---------- Create Student ----------

                StudentCreateRequest createReq = new StudentCreateRequest();

                createReq.setAdmissionNumber("ADM-200");
                createReq.setFirstName("Rahul");
                createReq.setLastName("Sharma");
                createReq.setGender(Gender.MALE);

                HttpEntity<StudentCreateRequest> createEntity = new HttpEntity<>(createReq, headers);

                ResponseEntity<StudentDto> createResp = restTemplate.exchange(
                                "/api/students",
                                HttpMethod.POST,
                                createEntity,
                                StudentDto.class);

                Assertions.assertThat(createResp.getStatusCode())
                                .isEqualTo(HttpStatus.OK);

                studentId = Objects.requireNonNull(createResp.getBody()).getId();

                // ---------- Update Student ----------

                StudentUpdateRequest updateReq = new StudentUpdateRequest();

                updateReq.setFirstName("Rahul Updated");
                updateReq.setContactNumber("9999999999");
                updateReq.setCity("Lucknow");

                updateReq.setPreviousSchoolName("ABC Public School");
                updateReq.setPreviousSchoolBoard("ICSE");
                updateReq.setPreviousClass("5");
                updateReq.setPreviousYearOfPassing(2023);
                updateReq.setTransferCertificateNumber("TC-12345");

                HttpEntity<StudentUpdateRequest> updateEntity = new HttpEntity<>(updateReq, headers);

                ResponseEntity<StudentDto> updateResp = restTemplate.exchange(
                                "/api/students/" + studentId,
                                HttpMethod.PUT,
                                updateEntity,
                                StudentDto.class);

                Assertions.assertThat(updateResp.getStatusCode())
                                .isEqualTo(HttpStatus.OK);

                // ---------- Fetch Student ----------

                HttpEntity<Void> getEntity = new HttpEntity<>(headers);

                ResponseEntity<StudentDto> getResp = restTemplate.exchange(
                                "/api/students/" + studentId,
                                HttpMethod.GET,
                                getEntity,
                                StudentDto.class);

                Assertions.assertThat(getResp.getStatusCode())
                                .isEqualTo(HttpStatus.OK);

                StudentDto updated = Objects.requireNonNull(getResp.getBody());

                Assertions.assertThat(updated).isNotNull();

                // ---------- Assertions ----------

                Assertions.assertThat(updated.getFirstName())
                                .isEqualTo("Rahul Updated");

                Assertions.assertThat(updated.getContactNumber())
                                .isEqualTo("9999999999");

                Assertions.assertThat(updated.getCity())
                                .isEqualTo("Lucknow");

                Assertions.assertThat(updated.getPreviousSchoolName())
                                .isEqualTo("ABC Public School");

                Assertions.assertThat(updated.getPreviousSchoolBoard())
                                .isEqualTo("ICSE");

                Assertions.assertThat(updated.getPreviousClass())
                                .isEqualTo("5");

                Assertions.assertThat(updated.getPreviousYearOfPassing())
                                .isEqualTo(2023);

                Assertions.assertThat(updated.getTransferCertificateNumber())
                                .isEqualTo("TC-12345");
        }

        // ------------------------------------------------

        @AfterEach
        void cleanup() {

                if (studentId != null) {
                        promotionRecordRepository
                                        .findByStudentIdOrderBySessionAsc(studentId)
                                        .forEach(promotionRecordRepository::delete);
                }

                if (studentId != null) {
                        studentEnrollmentRepository
                                        .findByStudentId(studentId, Pageable.unpaged())
                                        .forEach(studentEnrollmentRepository::delete);
                }

                if (studentId != null) {
                        feePaymentRepository
                                        .findByStudentId(studentId)
                                        .forEach(feePaymentRepository::delete);
                }

                if (studentId != null) {
                        assignmentRepository
                                        .findByStudentIdAndSession(studentId, "2025-26")
                                        .forEach(assignmentRepository::delete);
                }

                if (feeStructureId != null) {
                        feeStructureRepository.deleteById(feeStructureId);
                }

                if (feeTypeId != null) {
                        feeTypeRepository.deleteById(feeTypeId);
                }

                if (studentId != null) {
                        studentRepository.deleteById(studentId);
                }

                if (classId != null) {
                        schoolClassRepository.deleteById(classId);
                }

                if (schoolId != null) {
                        userRepository
                                        .findAll()
                                        .stream()
                                        .filter(u -> u.getSchool() != null &&
                                                        u.getSchool().getId().equals(schoolId))
                                        .forEach(userRepository::delete);
                }

                if (schoolId != null) {
                        schoolRepository.deleteById(schoolId);
                }
        }
}
